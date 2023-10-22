package com.dhruv.quick_apps

import android.content.Context

abstract class Action(val name: String, var onSelect: (Context)->Unit)

class TestAction(@JvmField val Name: String, @JvmField val OnSelect: (Context) -> Unit = { println(Name) }) : Action(Name, OnSelect)
