package de.dirkgerhardt.bahnfrei

import android.app.Application

/** PDFBox wird nicht mehr beim App-Start geladen, sondern beim ersten PDF-Import (siehe PdfParser.kt). */
class BahnFrei : Application()

