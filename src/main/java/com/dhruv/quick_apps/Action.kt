package com.dhruv.quick_apps

import android.content.Context

abstract class Action(val name: String, var onSelect: (Context)->Unit)