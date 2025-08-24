package com.mobyle.abbay.infra.startup

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.startup.Initializer
import com.mobyle.abbay.presentation.common.service.PlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PlayerInitializer : Initializer<Flow<MediaController>> {
    override fun create(context: Context): Flow<MediaController> {
        return flow {
            val controller = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlayerService::class.java))
            ).buildAsync()

            val player = controller.get()

            emit(player)
        }.flowOn(Dispatchers.IO)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
