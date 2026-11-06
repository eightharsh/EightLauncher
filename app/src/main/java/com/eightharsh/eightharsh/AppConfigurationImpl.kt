
package com.eightharsh.eightharsh

import android.os.Build
import com.eightharsh.eightharsh.core.domain.repository.AppConfiguration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

class AppConfigurationImpl @Inject constructor() : AppConfiguration {
    override val appVersion: String = BuildConfig.APP_VERSION
    override val appName: String = BuildConfig.APP_NAME
    override val sdkInt: Int = Build.VERSION.SDK_INT
}

@Suppress("unused") // It is used by hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAppConfiguration(
        impl: AppConfigurationImpl
    ): AppConfiguration
}
