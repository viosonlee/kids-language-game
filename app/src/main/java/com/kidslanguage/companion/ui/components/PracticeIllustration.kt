package com.kidslanguage.companion.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.kidslanguage.companion.core.content.isAssetImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled content can use either an inline pictogram or a validated local bitmap asset. */
@Composable
fun PracticeIllustration(reference: String, size: Dp, modifier: Modifier = Modifier) {
    if (!isAssetImage(reference)) {
        Text(reference, fontSize = size.value.sp, modifier = modifier)
        return
    }
    val context = LocalContext.current
    var bitmap by remember(reference) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(context, reference) {
        bitmap = try {
            withContext(Dispatchers.IO) {
                context.assets.open(reference).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Log.w("PracticeIllustration", "Unable to decode $reference")
            null
        }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, contentDescription = "练习图片", modifier = Modifier.size(size), contentScale = ContentScale.Fit) }
    }
}
