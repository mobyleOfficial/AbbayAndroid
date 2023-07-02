package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookEntity

class BooksLocalDataSourceImpl : BooksLocalDataSource {
    override fun getBooksList(): List<BookEntity> {
        return emptyList()
    }
}