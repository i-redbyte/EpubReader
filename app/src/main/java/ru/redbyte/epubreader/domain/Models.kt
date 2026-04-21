package ru.redbyte.epubreader.domain

import java.io.File

data class PreparedBook(
    val bookId: String,
    val title: String,
    val unpackRoot: File,
    val opfDir: File,
    val spineFiles: List<File>,
    val tocEntries: List<TocEntry>,
)

data class TocEntry(
    val title: String,
    val href: String,
    val depth: Int,
)

data class ReadingPosition(
    val bookId: String,
    val spineIndex: Int,
    val scrollRatio: Float,
)
