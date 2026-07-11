import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset

@Composable
fun Test() {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)))
    
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).drawBehind {
            rotate(rotation) {
                drawRect(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Red, Color.Blue, Color.Red),
                        center = center
                    ),
                    topLeft = Offset(-size.width, -size.height),
                    size = size.times(3f)
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(2.dp).background(Color.Black, RoundedCornerShape(22.dp)))
    }
}
