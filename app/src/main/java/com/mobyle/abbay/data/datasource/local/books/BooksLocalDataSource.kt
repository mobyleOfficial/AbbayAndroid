package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookEntity

interface BooksLocalDataSource {
    fun getBooksList(): List<BookEntity>
}