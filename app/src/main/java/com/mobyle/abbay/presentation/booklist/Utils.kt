package com.mobyle.abbay.presentation.booklist

import com.model.Book
import com.model.MultipleBooks

fun Book?.getBookPosition(): Int? = (this as? MultipleBooks)?.currentBookPosition
