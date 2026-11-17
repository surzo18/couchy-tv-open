package com.conreo.couchytv.ui

fun defaultHomePackage(context:android.content.Context):String?=context.packageManager.resolveActivity(
    android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName