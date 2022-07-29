package com.sunion.ikeyconnect.data

import com.sunion.ikeyconnect.R

enum class SunionTraits {
    AutoLock,
    Preamble,
    Passage,
    WiFi
}

enum class FirmwareModel(val data: Triple<Int, Int, List<SunionTraits>>) {
    KD0(Triple(R.string.model_kd0, R.drawable.ic_support_faq, listOf(SunionTraits.AutoLock))),
    KDW00(Triple(R.string.model_kdw00, R.drawable.ic_support_faq, listOf(SunionTraits.AutoLock, SunionTraits.WiFi))),
    KL0(Triple(R.string.model_kl0, R.drawable.ic_support_faq, listOf(SunionTraits.Passage))),
    TD0(Triple(R.string.model_td0, R.drawable.ic_support_faq, listOf(SunionTraits.AutoLock, SunionTraits.Preamble))),
    TL0(Triple(R.string.model_tl0, R.drawable.ic_support_faq, listOf(SunionTraits.Passage, SunionTraits.Preamble)))
}

fun getFirmwareModelTraits(name: String) = when(name){
    "KD0" -> FirmwareModel.KD0.data.third
    "KDW00" -> FirmwareModel.KDW00.data.third
    "KL0" -> FirmwareModel.KL0.data.third
    "TD0" -> FirmwareModel.TD0.data.third
    "TL0" -> FirmwareModel.TL0.data.third
    else -> FirmwareModel.KD0.data.third
}

fun getFirmwareModelUrl(nameId: Int) = when(nameId){
    R.string.model_kd0 -> "https://www.ikey-lock.com/FAQ/KD0.html"
    R.string.model_kdw00 -> "https://www.ikey-lock.com/FAQ/KD0.html"
    R.string.model_kl0 -> "https://www.ikey-lock.com/FAQ/KL0.html"
    R.string.model_td0 -> "https://www.ikey-lock.com/FAQ/TD0.html"
    R.string.model_tl0 -> "https://www.ikey-lock.com/FAQ/TL0.html"
    else -> "https://www.ikey-lock.com/FAQ/FAQ.html"
}

fun getFirmwareModelUrlByString(name: String) = when(name){
    "KD0" -> "https://www.ikey-lock.com/FAQ/KD0.html"
    "KDW00" -> "https://www.ikey-lock.com/FAQ/KDW00.html"
    "KL0" -> "https://www.ikey-lock.com/FAQ/KL0.html"
    "TD0" -> "https://www.ikey-lock.com/FAQ/TD0.html"
    "TL0" -> "https://www.ikey-lock.com/FAQ/TL0.html"
    else -> "https://www.ikey-lock.com/FAQ/FAQ.html"
}

fun getFirmwareModelList() = listOf<String>("KD0","KDW00","KL0","TD0","TL0")