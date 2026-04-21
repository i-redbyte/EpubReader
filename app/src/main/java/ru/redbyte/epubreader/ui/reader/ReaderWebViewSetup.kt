package ru.redbyte.epubreader.ui.reader

import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONObject

/**
 * EPUB XHTML often ships with multi-column CSS or fixed heights; older WebViews (e.g. API 29)
 * and emulator GPU paths can render those layouts as overlapping blocks. We force a single
 * reflow column and predictable viewport behavior; on emulators we prefer software rendering
 * to avoid compositor glitches.
 */
internal fun WebView.configureForEpubReading() {
    setBackgroundColor(Color.WHITE)
    if (isProbablyEmulator()) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    } else {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        loadsImagesAutomatically = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        textZoom = 100
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
}

internal fun WebView.injectEpubLayoutFixCss() {
    val css = EPUB_LAYOUT_FIX_CSS.trimIndent().replace(Regex("\\s+"), " ")
    val quoted = JSONObject.quote(css)
    val js = """
        (function(){
          try {
            var head = document.head || document.documentElement;
            var v = document.querySelector('meta[name="viewport"]');
            if (!v) {
              v = document.createElement('meta');
              v.setAttribute('name', 'viewport');
              head.insertBefore(v, head.firstChild);
            }
            v.setAttribute('content', 'width=device-width, initial-scale=1');
            if (document.getElementById('epubreader-layout-fix')) return;
            var s = document.createElement('style');
            s.id = 'epubreader-layout-fix';
            s.appendChild(document.createTextNode($quoted));
            head.appendChild(s);
          } catch (e) {}
        })();
    """.trimIndent()
    evaluateJavascript(js, null)
}

private fun isProbablyEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk" == Build.PRODUCT
        || Build.HARDWARE.contains("goldfish")
        || Build.HARDWARE.contains("ranchu")
        || Build.DEVICE.startsWith("generic")
        || Build.PRODUCT.contains("sdk_gphone")
        || Build.PRODUCT.contains("emulator")
        || Build.PRODUCT.contains("simulator")
}

private const val EPUB_LAYOUT_FIX_CSS = """
html, body {
  -webkit-column-count: 1 !important;
  column-count: 1 !important;
  column-width: auto !important;
  column-gap: normal !important;
  column-fill: auto !important;
}
body {
  width: 100% !important;
  max-width: 100% !important;
  min-height: 0 !important;
  height: auto !important;
  overflow-x: hidden !important;
  -webkit-text-size-adjust: 100% !important;
  text-size-adjust: 100% !important;
}
section, article, div, p, figure {
  max-width: 100% !important;
  box-sizing: border-box !important;
}
img, svg:not(:root), picture, video, object, embed {
  max-width: 100% !important;
  height: auto !important;
  box-sizing: border-box !important;
}
"""
