package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookEntity
import com.model.Book

fun Book.toEntity() = BookEntity(path, name, thumbnail, duration)