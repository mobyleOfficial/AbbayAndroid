package com.mobyle.abbay.presentation.utils.permissions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CheckPermissionsProviderModule {

    @Binds
    fun bindCheckPermissionsProvider(impl: CheckPermissionsProviderImpl): CheckPermissionsProvider
}
