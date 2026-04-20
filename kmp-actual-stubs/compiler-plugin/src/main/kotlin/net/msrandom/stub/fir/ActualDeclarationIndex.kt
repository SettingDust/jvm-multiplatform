package net.msrandom.stub.fir

import java.io.File

class ActualDeclarationIndex private constructor(
    private val classLikeDeclarations: Set<String>,
    private val callableDeclarations: Set<String>,
) {
    fun hasClassLike(packageName: String, name: String): Boolean = key(packageName, name) in classLikeDeclarations

    fun hasCallable(packageName: String, name: String): Boolean = key(packageName, name) in callableDeclarations

    companion object {
        private val packageRegex = Regex("^\\s*package\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
        private val classLikeRegex = Regex(
            "^\\s*(?:@[A-Za-z0-9_()., <>:=\\[\\]?]+\\s+|[A-Za-z0-9_<>]+\\s+)*actual\\s+(?:enum\\s+class|annotation\\s+class|value\\s+class|data\\s+class|sealed\\s+class|class|interface|object|typealias)\\s+([A-Za-z_][A-Za-z0-9_]*)",
            setOf(RegexOption.MULTILINE)
        )
        private val callableRegex = Regex(
            "^\\s*(?:@[A-Za-z0-9_()., <>:=\\[\\]?]+\\s+|[A-Za-z0-9_<>]+\\s+)*actual\\s+(?:fun|val|var)\\s+([A-Za-z_][A-Za-z0-9_]*)",
            setOf(RegexOption.MULTILINE)
        )

        fun fromSourceRoots(sourceRoots: Set<String>): ActualDeclarationIndex {
            val classLikes = hashSetOf<String>()
            val callables = hashSetOf<String>()

            for (rootPath in sourceRoots) {
                collect(File(rootPath), classLikes, callables)
            }

            return ActualDeclarationIndex(classLikes, callables)
        }

        private fun collect(path: File, classLikes: MutableSet<String>, callables: MutableSet<String>) {
            if (!path.exists()) return

            if (path.isDirectory) {
                path.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file -> collectFile(file, classLikes, callables) }
                return
            }

            if (path.isFile && path.extension == "kt") {
                collectFile(path, classLikes, callables)
            }
        }

        private fun collectFile(file: File, classLikes: MutableSet<String>, callables: MutableSet<String>) {
            val text = try {
                file.readText()
            } catch (_: Exception) {
                return
            }

            val packageName = packageRegex.find(text)?.groupValues?.get(1).orEmpty()

            classLikeRegex.findAll(text).forEach { match ->
                classLikes += key(packageName, match.groupValues[1])
            }
            callableRegex.findAll(text).forEach { match ->
                callables += key(packageName, match.groupValues[1])
            }
        }

        private fun key(packageName: String, name: String): String = "$packageName/$name"
    }
}