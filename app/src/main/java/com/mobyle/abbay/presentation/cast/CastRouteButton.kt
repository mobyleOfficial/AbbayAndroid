package com.mobyle.abbay.presentation.cast

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener

object CastRouteButton {
    fun create(context: Context, tint: Int): View? = runCatching {
        val castContext = CastContext.getSharedInstance(context.applicationContext)
        val theme = if (ColorUtils.calculateLuminance(tint) > HALF) {
            androidx.appcompat.R.style.Theme_AppCompat_NoActionBar
        } else {
            androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar
        }
        val button = MediaRouteButton(ContextThemeWrapper(context, theme))
        CastButtonFactory.setUpMediaRouteButton(context.applicationContext, button)
        val listener = CastStateListener { state -> button.visibility = visibilityOf(state) }
        button.visibility = visibilityOf(castContext.castState)
        button.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = castContext.addCastStateListener(listener)

            override fun onViewDetachedFromWindow(view: View) = castContext.removeCastStateListener(listener)
        })
        button
    }.getOrNull()

    private const val HALF = 0.5

    private fun visibilityOf(state: Int) = if (state == CastState.NO_DEVICES_AVAILABLE) View.GONE else View.VISIBLE
}
