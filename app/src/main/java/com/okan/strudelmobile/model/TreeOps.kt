package com.okan.strudelmobile.model

/** Immutable edits on the pattern tree. Every function returns a new root. */
object TreeOps {

    fun find(root: Chain, id: String): Chain? = pathTo(root, id)?.last()

    /** Root-to-target list of chains, or null if [id] isn't in the tree. */
    fun pathTo(root: Chain, id: String): List<Chain>? {
        if (root.id == id) return listOf(root)
        val group = root.source as? GroupSource ?: return null
        for (child in group.children) {
            val sub = pathTo(child, id)
            if (sub != null) return listOf(root) + sub
        }
        return null
    }

    /** Which chain owns transform [transformId] (directly or nested in a function arg), if any. */
    fun chainOfTransform(root: Chain, transformId: String): Chain? {
        if (containsTransform(root.transforms, transformId)) return root
        val group = root.source as? GroupSource ?: return null
        return group.children.firstNotNullOfOrNull { chainOfTransform(it, transformId) }
    }

    fun findTransform(root: Chain, transformId: String): Transform? =
        chainOfTransform(root, transformId)?.let { findIn(it.transforms, transformId) }

    private fun containsTransform(list: List<Transform>, id: String): Boolean = findIn(list, id) != null

    private fun findIn(list: List<Transform>, id: String): Transform? {
        for (t in list) {
            if (t.id == id) return t
            for (arg in t.args) if (arg is Arg.Fn) findIn(arg.transforms, id)?.let { return it }
        }
        return null
    }

    /** `layer(...)`: append another function argument. */
    fun addFnArg(root: Chain, transformId: String): Chain =
        updateTransform(root, transformId) { it.copy(args = it.args + Arg.Fn()) }

    /** `layer(...)`: drop one function argument (never the last). */
    fun removeFnArg(root: Chain, transformId: String, argIndex: Int): Chain =
        updateTransform(root, transformId) { t ->
            if (t.args.count { it is Arg.Fn } <= 1) t else t.copy(args = t.args.filterIndexed { i, _ -> i != argIndex })
        }

    /** Add a block inside a function argument: `every(4, x => x.<new>)`. */
    fun addNestedTransform(root: Chain, parentTransformId: String, argIndex: Int, transform: Transform): Chain =
        updateTransform(root, parentTransformId) { t ->
            t.copy(args = t.args.mapIndexed { i, a ->
                if (i == argIndex && a is Arg.Fn) a.copy(transforms = a.transforms + transform) else a
            })
        }

    fun update(root: Chain, id: String, edit: (Chain) -> Chain): Chain {
        if (root.id == id) return edit(root)
        val group = root.source as? GroupSource ?: return root
        return root.copy(source = group.copy(children = group.children.map { update(it, id, edit) }))
    }

    fun setPattern(root: Chain, id: String, pattern: String): Chain = update(root, id) { c ->
        val src = c.source as? MiniSource ?: return@update c
        c.copy(source = src.copy(pattern = pattern))
    }

    fun setSourceFn(root: Chain, id: String, fn: String): Chain = update(root, id) { c ->
        when (val src = c.source) {
            is MiniSource -> c.copy(source = src.copy(fn = fn))
            is GroupSource -> c.copy(source = src.copy(fn = fn))
        }
    }

    fun addTransform(root: Chain, chainId: String, transform: Transform): Chain =
        update(root, chainId) { it.copy(transforms = it.transforms + transform) }

    fun removeTransform(root: Chain, transformId: String): Chain = mapTransforms(root) { list ->
        list.filterNot { it.id == transformId }
    }

    fun updateTransform(root: Chain, transformId: String, edit: (Transform) -> Transform): Chain =
        mapTransforms(root) { list -> list.map { if (it.id == transformId) edit(it) else it } }

    fun setArg(root: Chain, transformId: String, index: Int, arg: Arg): Chain =
        updateTransform(root, transformId) { t ->
            t.copy(args = t.args.mapIndexed { i, a -> if (i == index) arg else a })
        }

    fun moveTransform(root: Chain, transformId: String, delta: Int): Chain = mapTransforms(root) { list ->
        val i = list.indexOfFirst { it.id == transformId }
        val j = i + delta
        if (i < 0 || j !in list.indices) list
        else list.toMutableList().apply { add(j, removeAt(i)) }
    }

    /** Wrap a mini-notation chain into a group with itself as the first layer. */
    fun wrapInGroup(root: Chain, id: String, groupFn: String): Chain = update(root, id) { c ->
        val inner = Chain(source = c.source, transforms = emptyList())
        Chain(id = c.id, source = GroupSource(groupFn, listOf(inner)), transforms = c.transforms)
    }

    fun addChild(root: Chain, groupId: String, child: Chain): Chain = update(root, groupId) { c ->
        val g = c.source as? GroupSource ?: return@update c
        c.copy(source = g.copy(children = g.children + child))
    }

    fun removeChild(root: Chain, groupId: String, childId: String): Chain = update(root, groupId) { c ->
        val g = c.source as? GroupSource ?: return@update c
        val remaining = g.children.filterNot { it.id == childId }
        when (remaining.size) {
            0 -> c.copy(source = MiniSource("s", "bd sd"))
            // A group of one collapses back into a plain chain, keeping both
            // sets of transforms in playing order (inner first).
            1 -> c.copy(source = remaining[0].source, transforms = remaining[0].transforms + c.transforms)
            else -> c.copy(source = g.copy(children = remaining))
        }
    }

    fun moveChild(root: Chain, groupId: String, childId: String, delta: Int): Chain = update(root, groupId) { c ->
        val g = c.source as? GroupSource ?: return@update c
        val i = g.children.indexOfFirst { it.id == childId }
        val j = i + delta
        if (i < 0 || j !in g.children.indices) c
        else c.copy(source = g.copy(children = g.children.toMutableList().apply { add(j, removeAt(i)) }))
    }

    /** All mini-source chains in document order. */
    fun miniSources(root: Chain): List<Chain> = when (val s = root.source) {
        is MiniSource -> listOf(root)
        is GroupSource -> s.children.flatMap { miniSources(it) }
    }

    /**
     * Applies [f] to every transform list in the tree: each chain's own list
     * and every nested function-argument list, innermost first.
     */
    private fun mapTransforms(root: Chain, f: (List<Transform>) -> List<Transform>): Chain {
        val src = root.source
        val newSource = if (src is GroupSource) src.copy(children = src.children.map { mapTransforms(it, f) }) else src
        return root.copy(source = newSource, transforms = mapList(root.transforms, f))
    }

    private fun mapList(list: List<Transform>, f: (List<Transform>) -> List<Transform>): List<Transform> {
        val inner = list.map { t ->
            if (t.args.none { it is Arg.Fn }) t
            else t.copy(args = t.args.map { a -> if (a is Arg.Fn) a.copy(transforms = mapList(a.transforms, f)) else a })
        }
        return f(inner)
    }
}
