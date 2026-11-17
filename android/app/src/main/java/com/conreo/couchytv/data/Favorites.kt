package com.conreo.couchytv.data

/** One atomic configuration edit: a replaced app always returns to the other apps. */
object Favorites {
    fun visible(config: LauncherConfig, apps: List<AppEntry>): List<AppEntry> =
        AppRepository.categorize(apps, config).firstOrNull { it.first.id == "favorites" }
            ?.second.orEmpty().filter { !Inputs.isInput(it.pkg) && it.pkg !in config.hidden }.take(4)

    fun toggle(config: LauncherConfig, apps: List<AppEntry>, app: AppEntry, replace: String? = null): LauncherConfig {
        require(!Inputs.isInput(app.pkg))
        val current = visible(config, apps).map { it.pkg }
        val desired = when {
            app.pkg in current -> current - app.pkg
            current.size < 4 -> current + app.pkg
            replace in current -> current.map { if (it == replace) app.pkg else it }
            else -> error("Vyber aplikáciu, ktorú chceš nahradiť.")
        }
        val memberships = config.sections.mapValues { (_, value) -> value - "favorites" }.toMutableMap()
        apps.filter { it.pkg in desired }.forEach {
            memberships[it.pkg] = AppRepository.sectionsOf(it, config) + "favorites"
        }
        return config.copy(
            sections = memberships,
            order = config.order + ("favorites" to desired),
            hidden = config.hidden - app.pkg - setOfNotNull(replace),
        )
    }
}
