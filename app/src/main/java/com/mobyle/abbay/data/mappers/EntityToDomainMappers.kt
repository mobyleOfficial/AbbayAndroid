package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookEntity
import com.model.Book

fun BookEntity.toDomain() = Book(path, name, thumbnail, duration)