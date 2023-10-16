package com.dhruv.quick_apps

abstract class Action(val name: String, val onSelect: ()->Unit)

class TestAction(@JvmField val Name: String, @JvmField val OnSelect: () -> Unit = {}) : Action(Name, OnSelect)
