package app.vazovsky.permaware.di

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import app.vazovsky.permaware.core.util.SystemTimeProvider
import app.vazovsky.permaware.core.util.TimeProvider
import app.vazovsky.permaware.data.db.ChangeDao
import app.vazovsky.permaware.data.db.PermissionWatchDatabase
import app.vazovsky.permaware.data.db.ScanDao
import app.vazovsky.permaware.data.db.SnapshotDao
import app.vazovsky.permaware.data.platform.InstalledAppsDataSource
import app.vazovsky.permaware.data.platform.PermissionInspector
import app.vazovsky.permaware.data.platform.PlatformInstalledAppsDataSource
import app.vazovsky.permaware.data.platform.PlatformPermissionInspector
import app.vazovsky.permaware.data.platform.PlatformSpecialAccessInspector
import app.vazovsky.permaware.data.platform.SpecialAccessInspector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providePackageManager(@ApplicationContext context: Context): PackageManager = context.packageManager

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PermissionWatchDatabase =
        Room.databaseBuilder(context, PermissionWatchDatabase::class.java, PermissionWatchDatabase.NAME)
            .build()

    @Provides
    fun provideScanDao(database: PermissionWatchDatabase): ScanDao = database.scanDao()

    @Provides
    fun provideSnapshotDao(database: PermissionWatchDatabase): SnapshotDao = database.snapshotDao()

    @Provides
    fun provideChangeDao(database: PermissionWatchDatabase): ChangeDao = database.changeDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindInstalledAppsDataSource(impl: PlatformInstalledAppsDataSource): InstalledAppsDataSource

    @Binds
    @Singleton
    abstract fun bindPermissionInspector(impl: PlatformPermissionInspector): PermissionInspector

    @Binds
    @Singleton
    abstract fun bindSpecialAccessInspector(impl: PlatformSpecialAccessInspector): SpecialAccessInspector

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
