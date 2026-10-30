package com.library

import com.model.Book
import com.model.LibraryRoot

/** One level of the folder view: sub-folders that hold books, and the books right here. */
data class FolderLevel(val folders: List<FolderEntry>, val books: List<Book>)

/** A folder of the tree; [rootId] null with a blank [path] is the group of books added as single files. */
data class FolderEntry(val name: String, val rootId: Long?, val path: String, val bookCount: Int)

/** Where the folder view is: a root (or the single files) and a path inside it; [rootId] null and no path is the top. */
data class FolderLocation(val rootId: Long? = null, val path: String = "", val isSingleFiles: Boolean = false) {
    val isTop: Boolean get() = rootId == null && !isSingleFiles

    /** The breadcrumb's segments below the root. */
    val segments: List<String> get() = path.split('/').filter(String::isNotEmpty)

    fun child(name: String) = copy(path = if (path.isEmpty()) name else "$path/$name")

    /** The location [depth] segments below the root (0 is the root itself). */
    fun upTo(depth: Int) = copy(path = segments.take(depth).joinToString("/"))

    companion object {
        val TOP = FolderLocation()
    }
}

/**
 * Builds the folder view from the books' locations. The top level lists the roots and, when there are any, the books
 * added as single files. A book folder is shown as a book in its parent folder, not as a folder. Pure logic.
 */
object FolderTree {
    private val naturalOrder = NaturalOrderComparator()

    fun level(roots: List<LibraryRoot>, books: List<Book>, location: FolderLocation): FolderLevel = when {
        location.isTop -> {
            val folders = roots.map { root -> FolderEntry(root.displayName, root.id, "", books.count { it.rootId == root.id }) }
                .filter { it.bookCount > 0 } + singleFiles(books)
            FolderLevel(folders.sortedWith(compareBy(naturalOrder) { it.name }), emptyList())
        }

        location.isSingleFiles -> FolderLevel(
            emptyList(),
            books.filter {
                it.rootId == null
            }.sortedWith(compareBy(naturalOrder) { it.name })
        )

        else -> folderLevel(books.filter { it.rootId == location.rootId }, location)
    }

    private fun folderLevel(inRoot: List<Book>, location: FolderLocation): FolderLevel {
        val here = inRoot.filter { parentOf(it.relativePath) == location.path }
        val prefix = if (location.path.isEmpty()) "" else "${location.path}/"
        val below = inRoot.filter { book -> book.relativePath.startsWith(prefix) && parentOf(book.relativePath) != location.path }
        val folders = below.groupBy { it.relativePath.removePrefix(prefix).substringBefore('/') }
            .map { (name, contained) -> FolderEntry(name, location.rootId, prefix + name, contained.size) }
        return FolderLevel(
            folders = folders.sortedWith(compareBy(naturalOrder) { it.name }),
            books = here.sortedWith(compareBy(naturalOrder) { it.name }),
        )
    }

    /** The location whose level lists [book]. */
    fun locationOf(book: Book): FolderLocation =
        if (book.rootId == null) FolderLocation(isSingleFiles = true) else FolderLocation(book.rootId, parentOf(book.relativePath))

    private fun singleFiles(books: List<Book>): List<FolderEntry> {
        val count = books.count { it.rootId == null }
        return if (count == 0) emptyList() else listOf(FolderEntry(SINGLE_FILES, null, "", count))
    }

    private fun parentOf(relativePath: String): String = relativePath.trim('/').substringBeforeLast('/', "")

    /** Name of the single-files entry; the UI shows a localized label for it. */
    const val SINGLE_FILES = ""
}
