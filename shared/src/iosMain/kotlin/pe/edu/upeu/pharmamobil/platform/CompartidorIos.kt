package pe.edu.upeu.pharmamobil.platform

import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        val activity = UIActivityViewController(listOf(texto), null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(activity, animated = true, completion = null)
    }
}
