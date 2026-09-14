package com.okan.strudelmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.okan.strudelmobile.ui.EditorViewModel
import com.okan.strudelmobile.ui.StrudelApp
import com.okan.strudelmobile.ui.StrudelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrudelTheme {
                StrudelApp(viewModel)
            }
        }
    }
}
