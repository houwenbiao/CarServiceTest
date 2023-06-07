/**
 * Created with JackHou
 * Date: 5/19/23
 * Time: 10:55 AM
 * Description:MainViewModel
 */

package com.jack.cartest.vm

import android.car.CarInfoManager
import android.car.VehicleAreaSeat
import android.car.VehiclePropertyIds.NIGHT_MODE
import android.car.hardware.CarPropertyValue
import android.car.hardware.CarSensorEvent
import android.car.hardware.CarSensorManager
import android.car.hardware.hvac.CarHvacManager
import android.car.hardware.hvac.CarHvacManager.ID_ZONED_TEMP_SETPOINT
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel constructor() : ViewModel() {

    companion object {
        const val TAG = "MainViewModel"
    }

    lateinit var mCarInfoManager: CarInfoManager
    lateinit var mCarSensorManager: CarSensorManager
    lateinit var mCarHvacManager: CarHvacManager
    lateinit var mCarPropertyManager: CarPropertyManager

    @Inject
    constructor(
        cim: CarInfoManager, csm: CarSensorManager, chm: CarHvacManager, cpm: CarPropertyManager
    ) : this() {
        mCarSensorManager = csm
        mCarInfoManager = cim
        mCarHvacManager = chm
        mCarPropertyManager = cpm
        registerListener()
    }


    var carInfo: MutableLiveData<String> = MutableLiveData()//车辆属性信息

    /**
     * 注册各种监听函数
     */
    private fun registerListener() {
        mCarSensorManager.registerListener(
            object : CarSensorManager.OnSensorChangedListener {
                override fun onSensorChanged(event: CarSensorEvent?) {
                    if (event?.sensorType == CarSensorManager.SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE) {
                        Log.d(TAG, "onSensorChanged: ${event.getEnvironmentData(null).temperature}")
                    }
                }

            },
            CarSensorManager.SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE,
            CarSensorManager.SENSOR_RATE_NORMAL
        )
        //空调温度变化监听
        mCarHvacManager.registerCallback(object : CarHvacManager.CarHvacEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>?) {
                if (value?.propertyId == ID_ZONED_TEMP_SETPOINT) {
                    Log.d(
                        TAG, "HVAC --> 空调温度设置成功！ temp = ${value.value}"
                    )
                }
                Log.d(
                    TAG,
                    "HVAC --> onChangeEvent:propertyId = ${value?.propertyId}, value = ${value?.value}"
                )
            }

            override fun onErrorEvent(propertyId: Int, area: Int) {
                Log.e(TAG, "HVAC --> onErrorEvent: area: $area, propertyId: $propertyId")
            }

        })
    }

    /**
     * 获取车辆信息
     */
    fun queryCarInfoEvent() {
        val bc = mCarInfoManager.evBatteryCapacity//电池容量
        val ct = mCarInfoManager.evConnectorTypes//充电连接器类型
        val fuel = mCarInfoManager.fuelTypes//燃料类型
        val fc = mCarInfoManager.fuelCapacity//燃料容量
        val manuFacturer = mCarInfoManager.manufacturer//制造商
        val mode = mCarInfoManager.model//车型
        val modelYear = mCarInfoManager.modelYearInInteger//车型年份
        val vehicleId = mCarInfoManager.vehicleId//车辆ID

        val info =
            "车辆信息如下： 电池容量 = $bc， 充电连接器类型 = $ct，燃料类型 = $fuel， 燃料容量 = $fc， 制造商 = $manuFacturer， 车型 = $mode, 车型年份 = $modelYear, 车辆ID = $vehicleId"
        carInfo.value = info

        if (mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_CAR_SPEED)) {
            val carSensorEvent =
                mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_CAR_SPEED)
            if (carSensorEvent != null) {
                val carSpeedData = carSensorEvent.getCarSpeedData(null)
                Log.d(TAG, "carSpeed: ${carSpeedData.carSpeed}")
            }
        } else {
            Log.w(TAG, "queryCarInfoEvent: SENSOR_TYPE_CAR_SPEED not supported")
        }

        if (mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE)) {
            val carSensorEvent =
                mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE)
            if (carSensorEvent != null) {
                val data = carSensorEvent.getEnvironmentData(null)
                Log.d(TAG, "carTemp: ${data.temperature}, timestamp: ${data.timestamp}")
            }
        } else {
            Log.w(TAG, "queryCarInfoEvent: SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE not supported")
        }

        val propertyValue:CarPropertyValue<Boolean> = mCarPropertyManager.getProperty(NIGHT_MODE, 0)
        if (propertyValue.status == CarPropertyValue.STATUS_AVAILABLE){
            Log.d(TAG, "$NIGHT_MODE = ${propertyValue.value}")
        }else{
            Log.w(TAG, "$NIGHT_MODE status UNAVAILABLE")
        }
    }


    /**
     * 温度自增1度
     */
    fun autoSetHvacTemp(areaSeat: Int = VehicleAreaSeat.SEAT_UNKNOWN, increment: Boolean): Boolean {
        val currentTemp = queryHvacTemp(areaSeat)
        if (currentTemp == -1f) {
            return false
        }
        val aid = getAreaId(areaSeat)
        if (aid == -1) {
            return false
        }
        var temp = currentTemp
        if (increment) {
            ++temp
        } else {
            --temp
        }

        if (hvacTempSettingInRange(areaSeat, temp)) {
            mCarHvacManager.setFloatProperty(ID_ZONED_TEMP_SETPOINT, aid, temp)
            return true
        }
        return false
    }

    /**
     * 获取指定座位的区域id
     */
    private fun getAreaId(areaSeat: Int = VehicleAreaSeat.SEAT_UNKNOWN): Int {
        var zone = areaSeat
        val carPropertyConfigs = mCarHvacManager.propertyList
        if (zone == VehicleAreaSeat.SEAT_UNKNOWN) {
            return -1
        }
        run loopPropertyConfigs@{
            carPropertyConfigs.forEach {
                if (it.propertyId == ID_ZONED_TEMP_SETPOINT) { //获取设置温度
                    it.areaIds.forEach { aid ->
                        if (zone and aid == zone) {
                            zone = aid
                        }
                    }
                    return@loopPropertyConfigs
                }
            }
        }
        return zone
    }

    /**
     * 获取指定位置的温度
     * 出现错误时候返回-1
     */
    private fun queryHvacTemp(areaSeat: Int = VehicleAreaSeat.SEAT_UNKNOWN): Float {
        var temp = -1f
        var zone = areaSeat
        val carPropertyConfigs = mCarHvacManager.propertyList
        var support = false
        if (zone == VehicleAreaSeat.SEAT_UNKNOWN) {
            return temp
        }
        run loopPropertyConfigs@{
            carPropertyConfigs.forEach {
                if (it.propertyId == ID_ZONED_TEMP_SETPOINT) { //获取设置温度
                    it.areaIds.forEach { aid ->
                        if (zone and aid == zone) {
                            zone = aid
                            support = true
                        }
                    }
                    return@loopPropertyConfigs
                }
            }
        }
        if (support && mCarHvacManager.isPropertyAvailable(ID_ZONED_TEMP_SETPOINT, zone)) {
            temp = mCarHvacManager.getFloatProperty(ID_ZONED_TEMP_SETPOINT, zone)
        }
        return temp
    }


    /**
     * 判断设置的温度是否在合法范围内
     */
    private fun hvacTempSettingInRange(
        areaSeat: Int = VehicleAreaSeat.SEAT_UNKNOWN, temp: Float
    ): Boolean {
        var zone = areaSeat
        val carPropertyConfigs = mCarHvacManager.propertyList
        var max = 0f//温度最大值
        var min = 0f//温度最小值
        run loopPropertyConfig@{
            carPropertyConfigs.forEach {
                if (it.propertyId == ID_ZONED_TEMP_SETPOINT) { //获取设置温度
                    it.areaIds.forEach { aid ->
                        if (zone and aid == zone) {
                            zone = aid
                            Log.d(TAG, "hvacTempSettingInRange: $zone")
                            val te = it.getMaxValue(zone)
                            max = it.getMaxValue(zone) as Float
                            min = it.getMinValue(zone) as Float
                        }
                    }
                    return@loopPropertyConfig
                }
            }
        }
        if (temp in min..max) {
            return true
        }
        return false
    }



}