package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.BookThumbnail
import com.mobyle.abbay.presentation.common.widgets.SVGIcon
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.MultipleBooks

@Composable
fun BookItem(
    book: Book,
    isSelected: Boolean,
    progress: String,
    intermediaryProgress: Long,
    currentMediaIndex: Int,
    onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                onClick.invoke()
            },
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            BookThumbnail(book.thumbnail)
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    book.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Row {
                    if (book is MultipleBooks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                        ) {
                            SVGIcon(
                                path = R.drawable.ic_book,
                                description = "Book Icon",
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                            )

                            Text(
                                "$currentMediaIndex/${book.bookFileList.size - 1}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                    ) {
                        SVGIcon(
                            path = R.drawable.ic_clock,
                            description = "Clock Icon",
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                        )
                        val currentProgress = if (isSelected) {
                            progress
                        } else intermediaryProgress.plus(book.progress).toHHMMSS()

                        Text(
                            "$currentProgress/${book.duration.toHHMMSS()}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}