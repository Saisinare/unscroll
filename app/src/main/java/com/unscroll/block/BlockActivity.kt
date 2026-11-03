package com.unscroll.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unscroll.data.BlockStats
import com.unscroll.ui.PillButton
import com.unscroll.ui.Tag
import com.unscroll.ui.UnscrollTheme
import com.unscroll.ui.palette

class BlockActivity : ComponentActivity() {
    private var reason by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        setContent {
            UnscrollTheme {
                Column(
                    Modifier.fillMaxSize().background(palette.bg).systemBarsPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(112.dp).clip(CircleShape).background(palette.card), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(44.dp), tint = palette.ink)
                        }
                        Spacer(Modifier.height(28.dp))
                        Text("Take a breath", style = MaterialTheme.typography.headlineLarge, color = palette.ink)
                        Spacer(Modifier.height(10.dp))
                        Text(reason, style = MaterialTheme.typography.bodyLarge, color = palette.muted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Tag("${BlockStats.today.value} blocks today", palette.mint, palette.ink)
                    }
                    PillButton("Back to calm", ::finish, Modifier.fillMaxWidth())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
    }

    companion object {
        private const val EXTRA_REASON = "reason"

        fun show(context: Context, reason: String) {
            context.startActivity(
                Intent(context, BlockActivity::class.java)
                    .putExtra(EXTRA_REASON, reason)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
