package com.aarogya.sanjeevani.core.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** The one full-screen loading indicator every list/detail screen uses
 *  while its first load is in flight. */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
