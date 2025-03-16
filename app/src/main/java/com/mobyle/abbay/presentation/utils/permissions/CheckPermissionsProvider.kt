package com.mobyle.abbay.presentation.utils.permissions

interface CheckPermissionsProvider {
    fun isPermissionGranted(permission: String): Boolean

    fun areAllPermissionsGranted(permissions: List<String>): Boolean
}
