package compass.core

import compass.NavController

interface NavContext {
    fun owner(): NavController?
    fun controller(): NavController
}