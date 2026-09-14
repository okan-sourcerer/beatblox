package com.okan.strudelmobile.model

/**
 * Human-readable descriptions for the sounds Strudel prebakes. The sample
 * packs ship no metadata, but their naming is regular enough to decode:
 * drum machines are `<bank>_<drum abbreviation>`, VCSL names are the
 * instrument, mridangam names are stroke syllables.
 */
enum class SoundCategory(val label: String) {
    DRUMS("drums"),
    PERCUSSION("perc"),
    MELODIC("melodic"),
    SYNTH("synth"),
    FX("fx/voice"),
}

data class SoundDescription(
    val category: SoundCategory,
    val text: String,
    /** Pitched sounds are meant for `note(...)`; unpitched for `s(...)`. */
    val pitched: Boolean,
)

object SoundDescriptions {

    /** Standard Tidal/Strudel drum abbreviations. */
    val drumAbbrev: Map<String, String> = mapOf(
        "bd" to "bass drum (kick)",
        "sd" to "snare drum",
        "sn" to "snare drum",
        "hh" to "closed hi-hat",
        "oh" to "open hi-hat",
        "cp" to "hand clap",
        "rim" to "rimshot / side stick",
        "cr" to "crash cymbal",
        "rd" to "ride cymbal",
        "lt" to "low tom",
        "mt" to "mid tom",
        "ht" to "high tom",
        "cb" to "cowbell",
        "sh" to "shaker",
        "tb" to "tambourine",
        "perc" to "misc percussion hit",
        "fx" to "effect / noise hit",
        "misc" to "miscellaneous sound",
    )

    /** The better-known drum machines; everything else gets a decoded name. */
    private val banks: Map<String, String> = mapOf(
        "rolandtr808" to "Roland TR-808 (1980) — deep booming kick, the hip-hop / electro classic",
        "rolandtr909" to "Roland TR-909 (1983) — punchy kick and hats, the house / techno kit",
        "rolandtr707" to "Roland TR-707 (1985) — crisp 12-bit PCM drums, 80s pop / early house",
        "rolandtr727" to "Roland TR-727 (1985) — latin percussion sibling of the 707",
        "rolandtr606" to "Roland TR-606 (1981) — thin analog beatbox, acid / minimal",
        "rolandtr505" to "Roland TR-505 (1986) — budget PCM kit, lo-fi 80s",
        "rolandtr626" to "Roland TR-626 (1987) — 707 successor with more sounds",
        "rolandcompurhythm78" to "Roland CR-78 (1978) — early preset rhythm box, soft analog",
        "rolandcompurhythm1000" to "Roland CR-1000 — 80s preset rhythm machine",
        "rolandcompurhythm8000" to "Roland CR-8000 (1981) — analog rhythm machine, 808 cousin",
        "rolandmc303" to "Roland MC-303 (1996) — groovebox drum sounds",
        "rolandmc202" to "Roland MC-202 — analog synth used percussively",
        "rolandr8" to "Roland R-8 (1989) — realistic 16-bit drums",
        "linndrum" to "LinnDrum (1982) — the 80s pop sampled kit",
        "linnlm1" to "Linn LM-1 (1980) — first sampled drum machine, Prince-era",
        "linnlm2" to "Linn LM-2 / LinnDrum era samples",
        "linn9000" to "Linn 9000 (1984) — sampler/sequencer drums",
        "akailinn" to "Akai / Linn MPC-era drum samples",
        "akaimpc60" to "Akai MPC60 (1988) — gritty 12-bit sampler, 90s hip-hop",
        "akaixr10" to "Akai XR10 (1990) — 16-bit PCM drum machine",
        "mpc1000" to "Akai MPC1000 — modern MPC drum samples",
        "oberheimdmx" to "Oberheim DMX (1981) — 80s hip-hop / new wave kit",
        "emusp12" to "E-mu SP-12 (1985) — crunchy 12-bit hip-hop sampler",
        "emudrumulator" to "E-mu Drumulator (1983) — 8-bit drums, early 80s",
        "emumodular" to "E-mu modular synth percussion",
        "sequentialcircuitsdrumtracks" to "Sequential Drumtraks (1984) — 80s synth-pop kit",
        "sequentialcircuitstom" to "Sequential Tom (1985) — 80s sampled drums",
        "simmonssds5" to "Simmons SDS-5 (1981) — the 80s electronic tom sound",
        "simmonssds400" to "Simmons SDS-400 — electronic toms",
        "korgm1" to "Korg M1 (1988) — workstation PCM drums",
        "korgddm110" to "Korg DDM-110 (1984) — cheap digital kit",
        "korgkpr77" to "Korg KPR-77 (1983) — analog 606-alike",
        "korgkr55" to "Korg KR-55 (1979) — preset analog rhythm box",
        "korgminipops" to "Korg Mini Pops — 60s/70s organ rhythm box",
        "korgpoly800" to "Korg Poly-800 synth hits",
        "korgt3" to "Korg T3 workstation drums",
        "korgkrz" to "Korg KRZ drum sounds",
        "casiorz1" to "Casio RZ-1 (1986) — lo-fi 8-bit sampler drums",
        "casiosk1" to "Casio SK-1 (1985) — toy sampler, very lo-fi",
        "casiovl1" to "Casio VL-1 (1981) — tiny calculator-keyboard beats",
        "alesishr16" to "Alesis HR-16 (1987) — clean 16-bit 80s/90s drums",
        "alesissr16" to "Alesis SR-16 (1990) — ubiquitous budget drum machine",
        "bossdr55" to "Boss DR-55 (1979) — minimal analog rhythm box",
        "bossdr110" to "Boss DR-110 (1983) — last analog Boss box, 606-like",
        "bossdr220" to "Boss DR-220 (1986) — PCM pocket drum machine",
        "bossdr550" to "Boss DR-550 (1990) — 90s PCM kit",
        "yamaharx5" to "Yamaha RX5 (1986) — punchy 80s digital drums",
        "yamaharx21" to "Yamaha RX21 (1985) — simple PCM kit",
        "yamahary30" to "Yamaha RY30 (1991) — 90s drum machine",
        "yamaharm50" to "Yamaha RM50 (1992) — rack drum module",
        "yamahatg33" to "Yamaha TG33 (1990) — vector synth percussion",
        "moogconcertmatemg1" to "Moog Concertmate MG-1 — analog synth percussion",
        "doepferms404" to "Doepfer MS-404 — analog synth used percussively",
        "sergemodular" to "Serge modular — experimental analog hits",
        "rolandsystem100" to "Roland System-100 modular percussion",
        "rolandsh09" to "Roland SH-09 mono synth hits",
        "rolandd110" to "Roland D-110 (1988) — LA-synthesis drums",
        "rolandd70" to "Roland D-70 synth drums",
        "rolandjd990" to "Roland JD-990 (1993) — 90s synth module drums",
        "rolandmt32" to "Roland MT-32 (1987) — the classic PC-game sound module",
        "rolands50" to "Roland S-50 (1986) — 12-bit sampler drums",
        "rolandddr30" to "Roland DDR-30 — digital drum module",
        "rhodespolaris" to "Rhodes Polaris synth percussion",
        "rhythmace" to "Ace Tone Rhythm Ace — 60s/70s preset rhythm box",
        "univoxmicrorhythmer12" to "Univox Micro Rhythmer 12 — 70s rhythm box",
        "viscospacedrum" to "Visco Space Drum — 70s electronic drum synth",
        "soundmastersr88" to "Soundmaster SR-88 — 80s analog beatbox",
        "sakatadpm48" to "Sakata DPM48 — 80s digital drums",
        "xdrumlm8953" to "X-drum LM-8953 — 80s drum machine",
        "mfb512" to "MFB-512 — German analog drum computer",
        "ajkpercusyn" to "AJK Percusyn — analog percussion synth",
    )

    /** Bank display name + description; works for banks not in the curated map too. */
    fun describeBank(bank: String): String {
        banks[bank.lowercase()]?.let { return it }
        // "RolandTR909" -> "Roland TR909"; lowercase inputs stay as-is.
        val pretty = bank.replace(Regex("(?<=[a-z])(?=[A-Z0-9])"), " ")
        return "$pretty — drum machine sample set"
    }

    fun describe(name: String, type: String): SoundDescription {
        val lower = name.lowercase()

        synths[lower]?.let { return SoundDescription(SoundCategory.SYNTH, it, pitched = true) }
        if (type != "sample") return SoundDescription(SoundCategory.SYNTH, "synth voice", pitched = true)

        dirtSamples[lower]?.let { return it }
        mridangam[lower]?.let { return SoundDescription(SoundCategory.PERCUSSION, "mridangam (South Indian drum) stroke \"$it\"", pitched = false) }

        // Drum machine: <bank>_<abbrev>
        val us = lower.indexOf('_')
        if (us > 0) {
            val bank = lower.substring(0, us)
            val abbrev = lower.substring(us + 1)
            val drum = drumAbbrev[abbrev]
            if (drum != null && bank in banks) {
                return SoundDescription(SoundCategory.DRUMS, "$drum · ${describeBank(bank)}", pitched = false)
            }
        }
        // Bare abbreviations (EmuSP12 pack, or under a .bank())
        drumAbbrev[lower]?.let {
            return SoundDescription(SoundCategory.DRUMS, "$it (E-mu SP-12 pack; with .bank() the bank's version is used)", pitched = false)
        }

        return describeVcsl(lower)
    }

    private val synths = mapOf(
        "sine" to "pure sine wave — soft, sub-bass friendly",
        "triangle" to "triangle wave — mellow, flute-like (the default synth)",
        "square" to "square wave — hollow, chiptune / clarinet",
        "sawtooth" to "sawtooth wave — bright and buzzy, classic synth lead/bass",
        "supersaw" to "stacked detuned saws — big trance / rave lead",
        "pulse" to "pulse wave — square with adjustable width",
        "white" to "white noise — hiss, for hats and risers",
        "pink" to "pink noise — softer noise",
        "brown" to "brown noise — deep rumble",
        "crackle" to "vinyl-style crackle noise",
        "z_sine" to "sine wave (ZZFX)",
        "z_square" to "square wave (ZZFX)",
        "z_triangle" to "triangle wave (ZZFX)",
        "z_sawtooth" to "sawtooth wave (ZZFX)",
        "z_tan" to "tan wave (ZZFX) — harsh",
        "z_noise" to "noise (ZZFX)",
        "bytebeat" to "bytebeat expression synth",
    )

    private val dirtSamples = mapOf(
        "piano" to SoundDescription(SoundCategory.MELODIC, "sampled acoustic piano (Salamander) — pitched, use with note", pitched = true),
        "casio" to SoundDescription(SoundCategory.MELODIC, "cheesy Casio keyboard tones", pitched = true),
        "crow" to SoundDescription(SoundCategory.FX, "crow caws", pitched = false),
        "east" to SoundDescription(SoundCategory.PERCUSSION, "eastern / asian percussion hits", pitched = false),
        "insect" to SoundDescription(SoundCategory.FX, "insect chirps and buzzes", pitched = false),
        "jazz" to SoundDescription(SoundCategory.DRUMS, "jazz drum kit hits (brushes, ride, kick)", pitched = false),
        "metal" to SoundDescription(SoundCategory.PERCUSSION, "metallic clangs and hits", pitched = false),
        "numbers" to SoundDescription(SoundCategory.FX, "spoken numbers 0–9 (use n to pick)", pitched = false),
        "space" to SoundDescription(SoundCategory.FX, "spacey synth textures and sweeps", pitched = false),
        "wind" to SoundDescription(SoundCategory.FX, "wind noise gusts", pitched = false),
    )

    private val mridangam = mapOf(
        "ardha" to "ardha", "chaapu" to "chaapu (rim)", "dhi" to "dhi", "dhin" to "dhin (ringing bass)",
        "dhum" to "dhum (bass)", "gumki" to "gumki (pitch bend)", "ka" to "ka", "ki" to "ki",
        "na" to "na", "nam" to "nam", "ta" to "ta", "tha" to "tha", "thom" to "thom (open bass)",
    )

    /** VCSL (Versilian Community Sample Library): orchestral & world instruments. */
    private fun describeVcsl(name: String): SoundDescription {
        val parts = name.split('_')
        val base = parts[0]
        val mods = parts.drop(1).map { vcslModifier[it] ?: it }.joinToString(", ")
        val (cat, text, pitched) = vcslBase[base] ?: Triple(SoundCategory.PERCUSSION, base.replaceFirstChar { it.uppercase() }, false)
        val full = if (mods.isEmpty()) text else "$text ($mods)"
        return SoundDescription(cat, "$full · VCSL", pitched)
    }

    private val vcslModifier = mapOf(
        "stacc" to "staccato", "sus" to "sustained", "vib" to "vibrato", "acc" to "accented",
        "hard" to "hard mallets", "soft" to "soft mallets", "medium" to "medium mallets",
        "ff" to "loud", "pp" to "quiet", "bow" to "bowed", "pluck" to "plucked", "spiccato" to "spiccato",
        "tremolo" to "tremolo", "vibrato" to "vibrato", "roll" to "roll", "slow" to "slow",
        "mallet" to "mallet", "rim" to "rim", "stick" to "stick", "hi" to "high", "low" to "low",
        "modern" to "modern", "large" to "large", "small" to "small", "loud" to "loud", "quiet" to "quiet",
        "pedal" to "pedal", "full" to "full", "4inch" to "4' stop", "8inch" to "8' stop",
        "alto" to "alto", "bass" to "bass", "soprano" to "soprano", "tenor" to "tenor", "bowed" to "bowed",
    )

    private val vcslBase: Map<String, Triple<SoundCategory, String, Boolean>> = mapOf(
        "agogo" to Triple(SoundCategory.PERCUSSION, "agogô bells (Brazilian)", false),
        "anvil" to Triple(SoundCategory.PERCUSSION, "anvil hit", false),
        "balafon" to Triple(SoundCategory.MELODIC, "balafon — West African wooden xylophone", true),
        "ballwhistle" to Triple(SoundCategory.FX, "ball whistle", false),
        "bassdrum1" to Triple(SoundCategory.DRUMS, "orchestral bass drum", false),
        "bassdrum2" to Triple(SoundCategory.DRUMS, "orchestral bass drum (alt)", false),
        "belltree" to Triple(SoundCategory.PERCUSSION, "bell tree glissando", false),
        "bongo" to Triple(SoundCategory.PERCUSSION, "bongos", false),
        "brakedrum" to Triple(SoundCategory.PERCUSSION, "brake drum (metal clang)", false),
        "cabasa" to Triple(SoundCategory.PERCUSSION, "cabasa shaker", false),
        "cajon" to Triple(SoundCategory.PERCUSSION, "cajón box drum", false),
        "clap" to Triple(SoundCategory.DRUMS, "hand clap", false),
        "clash" to Triple(SoundCategory.PERCUSSION, "clash cymbals", false),
        "clash2" to Triple(SoundCategory.PERCUSSION, "clash cymbals (alt)", false),
        "clave" to Triple(SoundCategory.PERCUSSION, "claves", false),
        "clavisynth" to Triple(SoundCategory.MELODIC, "clavinet-style synth", true),
        "conga" to Triple(SoundCategory.PERCUSSION, "congas", false),
        "cowbell" to Triple(SoundCategory.PERCUSSION, "cowbell", false),
        "dantranh" to Triple(SoundCategory.MELODIC, "đàn tranh — Vietnamese zither", true),
        "darbuka" to Triple(SoundCategory.PERCUSSION, "darbuka goblet drum", false),
        "didgeridoo" to Triple(SoundCategory.MELODIC, "didgeridoo drone", true),
        "fingercymbal" to Triple(SoundCategory.PERCUSSION, "finger cymbals", false),
        "flexatone" to Triple(SoundCategory.FX, "flexatone (wobbly metal)", false),
        "fmpiano" to Triple(SoundCategory.MELODIC, "FM electric piano (DX-style)", true),
        "folkharp" to Triple(SoundCategory.MELODIC, "folk harp", true),
        "framedrum" to Triple(SoundCategory.PERCUSSION, "frame drum", false),
        "glockenspiel" to Triple(SoundCategory.MELODIC, "glockenspiel", true),
        "gong" to Triple(SoundCategory.PERCUSSION, "gong", false),
        "gong2" to Triple(SoundCategory.PERCUSSION, "gong (alt)", false),
        "guiro" to Triple(SoundCategory.PERCUSSION, "güiro scraper", false),
        "handbells" to Triple(SoundCategory.MELODIC, "handbells", true),
        "handchimes" to Triple(SoundCategory.MELODIC, "hand chimes", true),
        "harmonica" to Triple(SoundCategory.MELODIC, "harmonica", true),
        "harp" to Triple(SoundCategory.MELODIC, "concert harp", true),
        "hihat" to Triple(SoundCategory.DRUMS, "acoustic hi-hat", false),
        "kalimba" to Triple(SoundCategory.MELODIC, "kalimba thumb piano", true),
        "kalimba2" to Triple(SoundCategory.MELODIC, "kalimba (alt 2)", true),
        "kalimba3" to Triple(SoundCategory.MELODIC, "kalimba (alt 3)", true),
        "kalimba4" to Triple(SoundCategory.MELODIC, "kalimba (alt 4)", true),
        "kalimba5" to Triple(SoundCategory.MELODIC, "kalimba (alt 5)", true),
        "kawai" to Triple(SoundCategory.MELODIC, "Kawai upright piano", true),
        "marimba" to Triple(SoundCategory.MELODIC, "marimba", true),
        "marktrees" to Triple(SoundCategory.PERCUSSION, "mark tree (wind chimes)", false),
        "ocarina" to Triple(SoundCategory.MELODIC, "ocarina", true),
        "oceandrum" to Triple(SoundCategory.FX, "ocean drum (wave sound)", false),
        "organ" to Triple(SoundCategory.MELODIC, "reed organ", true),
        "piano1" to Triple(SoundCategory.MELODIC, "grand piano", true),
        "pipeorgan" to Triple(SoundCategory.MELODIC, "pipe organ", true),
        "psaltery" to Triple(SoundCategory.MELODIC, "psaltery (zither)", true),
        "ratchet" to Triple(SoundCategory.FX, "ratchet", false),
        "recorder" to Triple(SoundCategory.MELODIC, "recorder (flute)", true),
        "sax" to Triple(SoundCategory.MELODIC, "saxophone", true),
        "saxello" to Triple(SoundCategory.MELODIC, "saxello (curved soprano sax)", true),
        "shaker" to Triple(SoundCategory.PERCUSSION, "shaker", false),
        "siren" to Triple(SoundCategory.FX, "hand-crank siren", false),
        "slapstick" to Triple(SoundCategory.PERCUSSION, "slapstick / whip", false),
        "sleighbells" to Triple(SoundCategory.PERCUSSION, "sleigh bells", false),
        "slitdrum" to Triple(SoundCategory.PERCUSSION, "slit drum (wooden)", false),
        "snare" to Triple(SoundCategory.DRUMS, "acoustic snare", false),
        "steinway" to Triple(SoundCategory.MELODIC, "Steinway grand piano", true),
        "strumstick" to Triple(SoundCategory.MELODIC, "strumstick (3-string dulcimer)", true),
        "super64" to Triple(SoundCategory.MELODIC, "Super 64 accordion", true),
        "sus" to Triple(SoundCategory.PERCUSSION, "suspended cymbal", false),
        "tambourine" to Triple(SoundCategory.PERCUSSION, "tambourine", false),
        "tambourine2" to Triple(SoundCategory.PERCUSSION, "tambourine (alt)", false),
        "timpani" to Triple(SoundCategory.DRUMS, "timpani", true),
        "timpani2" to Triple(SoundCategory.DRUMS, "timpani (alt)", true),
        "tom" to Triple(SoundCategory.DRUMS, "concert tom", false),
        "tom2" to Triple(SoundCategory.DRUMS, "concert tom (alt)", false),
        "trainwhistle" to Triple(SoundCategory.FX, "train whistle", false),
        "triangles" to Triple(SoundCategory.PERCUSSION, "triangle", false),
        "tubularbells" to Triple(SoundCategory.MELODIC, "tubular bells", true),
        "tubularbells2" to Triple(SoundCategory.MELODIC, "tubular bells (alt)", true),
        "vibraphone" to Triple(SoundCategory.MELODIC, "vibraphone", true),
        "vibraslap" to Triple(SoundCategory.PERCUSSION, "vibraslap rattle", false),
        "wineglass" to Triple(SoundCategory.MELODIC, "wine glass (bowed rim)", true),
        "woodblock" to Triple(SoundCategory.PERCUSSION, "woodblock", false),
        "xylophone" to Triple(SoundCategory.MELODIC, "xylophone", true),
    )
}
