/**
 * Created with JackHou
 * Date: 5/19/23
 * Time: 2:41 PM
 * Description:提供Car相关服务的module
 */

package com.jack.cartest.di.module

import android.car.Car
import android.car.CarInfoManager
import android.car.hardware.CarSensorManager
import android.car.hardware.hvac.CarHvacManager
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CarServiceModule {

    companion object {
        const val TAG = "CarServiceModule"
    }

    @Provides
    @Singleton
    fun providerCarService(@ApplicationContext ctx: Context): Car? {
        var mCarApiClient: Car? = null
        if (ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            val workHandler = Handler(ctx.mainLooper) {
                Log.d(TAG, "workHandler: $it")
                true
            }
            mCarApiClient =
                Car.createCar(ctx, workHandler, 3000, object : Car.CarServiceLifecycleListener {
                    override fun onLifecycleChanged(car: Car?, ready: Boolean) {
                        if (ready) {
                            Log.d(TAG, "onLifecycleChanged: CarService连接成功！")
                        } else {
                            Log.w(TAG, "onLifecycleChanged: CarService连接失败！")
                        }
                    }
                })
        } else {
            Log.d(TAG, "No Feature Automotive")
        }
        return mCarApiClient
    }

    @Provides
    @Singleton
    fun providerCarPropertyManager(car: Car?): CarPropertyManager {
        return car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    }

    @Provides
    @Singleton
    fun providerCarInfoManager(car: Car?): CarInfoManager {
        return car?.getCarManager(Car.INFO_SERVICE) as CarInfoManager
    }

    @Provides
    @Singleton
    fun providerCarSensorManager(car: Car?): CarSensorManager {
        return car?.getCarManager(Car.SENSOR_SERVICE) as CarSensorManager
    }


    @Provides
    @Singleton
    fun providerCarHvacManager(car: Car?): CarHvacManager {
        return car?.getCarManager(Car.HVAC_SERVICE) as CarHvacManager
    }
}