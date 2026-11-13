package com.metrowin.keyboard.settings

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import com.metrowin.keyboard.ui.dp
import androidx.preference.PreferenceFragmentCompat
import com.metrowin.keyboard.K
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import com.metrowin.keyboard.engine.LanguageEngine
import java.util.concurrent.Executors

/** Shared plumbing of the settings screens: a preference file, a title, and quick access to the app. */
abstract class BaseFragment(private val xml: Int, private val title: Int) : PreferenceFragmentCompat() {
    protected val app get() = KeyboardApp.of(requireContext())
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xml, rootKey)
        setup()
        // No room kept for icons: the screens have none.
        fun flat(g: androidx.preference.PreferenceGroup) {
            g.isIconSpaceReserved = false
            for (i in 0 until g.preferenceCount) preferenceScreen.let { g.getPreference(i) }.let { p -> p.isIconSpaceReserved = false; if (p is androidx.preference.PreferenceGroup) flat(p) }
        }
        flat(preferenceScreen)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(null)
    }

    abstract fun setup()

    override fun onResume() { super.onResume(); requireActivity().setTitle(title) }

    protected fun click(key: String, block: () -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener { block(); true }
    }

    protected fun toast(res: Int) = Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
    protected fun toast(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()

    /** Runs [work] off the main thread and then [done] on it, if the screen is still there. */
    protected fun <T> background(work: () -> T, done: (T) -> Unit) {
        val ctx = requireContext().applicationContext
        io.execute {
            val r = work()
            android.os.Handler(ctx.mainLooper).post { if (isAdded) done(r) }
        }
    }

    protected fun chooseOne(titleRes: Int, labels: List<String>, onPick: (Int) -> Unit) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(titleRes).setItems(labels.toTypedArray()) { _, i -> onPick(i) }.setNegativeButton(R.string.cancel, null).show()
    }
}

/** A full backup of the app in one file, and its restore. */
class BackupFragment : BaseFragment(R.xml.prefs_backup, R.string.screen_backup) {
    private val create = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = requireContext().applicationContext
        background({ runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { com.metrowin.keyboard.backup.Backup.write(ctx, it) } }.isSuccess }) { ok ->
            toast(if (ok) R.string.backup_done else R.string.backup_failed)
        }
    }

    private val pick = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.backup_restore_title).setMessage(R.string.backup_restore_confirm)
            .setPositiveButton(R.string.backup_restore_go) { _, _ -> restore(uri) }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun restore(uri: android.net.Uri) {
        val ctx = requireContext().applicationContext
        background({ runCatching { ctx.contentResolver.openInputStream(uri)!!.use { com.metrowin.keyboard.backup.Backup.restore(ctx, it) } }.exceptionOrNull().let { Pair(it == null, it) } }) { (ok, error) ->
            if (!ok) {
                // Say which: not one of ours, a damaged file, or a restore that could not finish (nothing is changed in any case).
                toast(when (error) {
                    is com.metrowin.keyboard.backup.Backup.Invalid, is java.util.zip.ZipException, is org.json.JSONException -> R.string.backup_invalid
                    is com.metrowin.keyboard.backup.Backup.Incomplete, is java.io.EOFException, is java.io.FileNotFoundException, is SecurityException -> R.string.backup_unreadable
                    else -> R.string.backup_restore_failed
                })
                return@background
            }
            toast(R.string.backup_restored)
            // Everything that was loaded (settings, clipboard, dictionaries) is now out of date: the app starts again from the files.
            android.os.Handler(ctx.mainLooper).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 1200)
        }
    }

    override fun setup() {
        click("backup_create") { create.launch("metrowin-keyboard-backup-" + java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date()) + ".zip") }
        click("backup_restore") { pick.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
    }
}

class LanguagesFragment : BaseFragment(R.xml.prefs_languages, R.string.screen_languages) {
    private var pendingLang: String? = null

    private val openDictionary = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = pendingLang
        if (uri == null || id == null) return@registerForActivityResult
        val layout = app.layouts.get(id) ?: return@registerForActivityResult
        val ctx = requireContext().applicationContext
        background({ runCatching { ctx.contentResolver.openInputStream(uri)!!.use { LanguageEngine.importDictionary(ctx, layout.dictionaryId, layout.locale, it) } } }) { result ->
            val count = result.getOrDefault(0)
            if (count > 0) { app.reloadEngines(); toast(getString(R.string.import_done_words, count)) }
            else toast(if (result.isFailure) R.string.import_failed else R.string.import_dictionary_empty)
        }
    }

    private val openLayout = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = runCatching { requireContext().contentResolver.openInputStream(uri)!!.use { it.readBytes().take(64 * 1024).toByteArray().toString(Charsets.UTF_8) } }.getOrNull()
        val layout = text?.let { app.layouts.import(it) }
        when {
            layout != null -> toast(getString(R.string.import_layout_done, layout.name))
            text == null -> toast(R.string.import_failed)
            else -> toast(R.string.import_layout_invalid)
        }
    }

    override fun setup() {
        click("languages_pick") {
            val all = app.layouts.all().values.toList()
            val current = app.prefs.languages.toMutableSet()
            val checked = BooleanArray(all.size) { all[it].id in current }
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.pref_languages_title)
                .setMultiChoiceItems(all.map { it.name }.toTypedArray(), checked) { _, i, on -> checked[i] = on }
                .setPositiveButton(R.string.ok) { _, _ ->
                    val picked = all.filterIndexed { i, _ -> checked[i] }.map { it.id }.ifEmpty { listOf("en") }
                    app.prefs.languages = picked
                    if (app.prefs.currentLanguage !in picked) app.prefs.currentLanguage = picked.first()
                }
                .setNegativeButton(R.string.cancel, null).show()
        }
        click("import_dictionary") {
            val all = app.layouts.all().values.toList()
            chooseOne(R.string.dialog_pick_language, all.map { it.name }) { i -> pendingLang = all[i].id; openDictionary.launch(arrayOf("*/*")) }
        }
        click("dictionary_sources") {
            val sources = listOf(
                "Hunspell – LibreOffice dictionaries" to "https://github.com/LibreOffice/dictionaries",
                "Hunspell – wooorm/dictionaries (many languages)" to "https://github.com/wooorm/dictionaries",
                "FrequencyWords (frequency lists)" to "https://github.com/hermitdave/FrequencyWords",
                "Wiktionary frequency lists" to "https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists",
                "Leipzig Corpora Collection (word lists)" to "https://wortschatz.uni-leipzig.de/en/download",
            )
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.pref_dictionary_sources_title).setMessage(R.string.dictionary_sources_note)
                .setItems(sources.map { it.first }.toTypedArray()) { _, i ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sources[i].second))) }
                }
                .setNegativeButton(R.string.cancel, null).show()
        }
        click("remove_dictionary") {
            val withImport = app.layouts.all().values.filter { LanguageEngine.dictionaryFile(requireContext(), it.dictionaryId).exists() }
            if (withImport.isEmpty()) { toast(R.string.nothing_to_remove); return@click }
            chooseOne(R.string.dialog_pick_imported, withImport.map { it.name }) { i ->
                LanguageEngine.removeImportedDictionary(requireContext(), withImport[i].dictionaryId); app.reloadEngines(); toast(R.string.removed)
            }
        }
        click("import_layout") { openLayout.launch(arrayOf("*/*")) }
        click("remove_layout") {
            val mine = app.layouts.all().values.filter { !it.builtIn }
            if (mine.isEmpty()) { toast(R.string.nothing_to_remove); return@click }
            chooseOne(R.string.dialog_pick_imported, mine.map { it.name }) { i -> app.layouts.remove(mine[i].id); toast(R.string.removed) }
        }
    }
}

class AppearanceFragment : BaseFragment(R.xml.prefs_appearance, R.string.screen_appearance) {
    // The preview and the pickers at the top redraw as soon as a look, mode, font, weight or colour changes.
    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        findPreference<KeyboardPreview>("appearance_preview")?.refresh()
        for (key in listOf("theme_style", "theme_mode", "key_font", "key_weight", "accent_pick")) (findPreference<Preference>(key) as? PickerRefresh)?.refresh()
    }
    override fun onStart() { super.onStart(); app.prefs.sp.registerOnSharedPreferenceChangeListener(listener) }
    override fun onStop() { app.prefs.sp.unregisterOnSharedPreferenceChangeListener(listener); super.onStop() }

    override fun setup() {
        findPreference<AccentPicker>("accent_pick")?.onCustom = { showAccentDialog() }
    }

    /** One colour of your own for the keyboard's accent; without it the look decides (Modern Droid follows the wallpaper). */
    private fun showAccentDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.accent_custom_hint); setSingleLine()
            app.prefs.customAccent.takeIf { it != 0 }?.let { setText(String.format("#%06X", it and 0xFFFFFF)) }
        }
        val box = android.widget.FrameLayout(ctx).apply { setPadding(ctx.dp(24f).toInt(), ctx.dp(8f).toInt(), ctx.dp(24f).toInt(), 0); addView(input) }
        MaterialAlertDialogBuilder(ctx).setTitle(R.string.accent_dialog_title).setMessage(R.string.accent_dialog_message).setView(box)
            .setPositiveButton(R.string.ok) { _, _ ->
                val parsed = runCatching { Color.parseColor(input.text.toString().trim().let { if (it.startsWith("#")) it else "#$it" }) }.getOrNull()
                if (parsed == null) toast(R.string.accent_invalid) else app.prefs.sp.edit().putInt(K.CUSTOM_ACCENT, parsed or 0xFF000000.toInt()).apply()
            }
            .setNeutralButton(R.string.accent_reset) { _, _ -> app.prefs.sp.edit().putInt(K.CUSTOM_ACCENT, 0).putBoolean(K.DYNAMIC_COLOR, true).apply() }
            .setNegativeButton(R.string.cancel, null).show()
    }
}

class SizeFragment : BaseFragment(R.xml.prefs_size, R.string.screen_size) {
    // The preview redraws as soon as a slider or switch changes.
    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> findPreference<KeyboardPreview>("size_preview")?.refresh() }

    override fun setup() {
        click("size_try") { startActivity(Intent(requireContext(), TestActivity::class.java)) }
        click("size_calibrate") { startActivity(Intent(requireContext(), CalibrationActivity::class.java)) }
        click("size_calibrate_reset") {
            app.prefs.sp.edit().remove(K.CALIB_BIAS).remove(K.CALIB_BIAS_LAND).remove(K.CALIB_SIGMA).apply(); toast(R.string.calib_reset_done)
        }
    }
    override fun onStart() { super.onStart(); app.prefs.sp.registerOnSharedPreferenceChangeListener(listener) }
    override fun onStop() { app.prefs.sp.unregisterOnSharedPreferenceChangeListener(listener); super.onStop() }
}

class TypingFragment : BaseFragment(R.xml.prefs_typing, R.string.screen_typing) {
    override fun setup() {
        // The quick button used to be able to open the translation and GIF tools: a phone that had one of them chosen goes back to the clipboard.
        findPreference<androidx.preference.ListPreference>(K.QUICK_TOOL)?.let { if (it.value !in it.entryValues.map { v -> v.toString() }) it.value = "clipboard" }
    }
}

class SuggestionsFragment : BaseFragment(R.xml.prefs_suggestions, R.string.screen_suggestions) {
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = requireContext().applicationContext
        background({
            runCatching {
                ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { w ->
                    for (id in app.prefs.languages) {
                        w.append("# ").append(id).append('\n')
                        app.engine(id).user.write(w)
                    }
                }
            }.isSuccess
        }) { ok -> toast(if (ok) R.string.export_done else R.string.export_failed) }
    }

    override fun setup() {
        click("export_learned") { export.launch("metrowin-keyboard-learned.txt") }
        click("clear_learned") {
            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.clear_learned_confirm)
                .setPositiveButton(R.string.ok) { _, _ -> app.clearLearned(); toast(R.string.cleared) }
                .setNegativeButton(R.string.cancel, null).show()
        }
    }
}

class FeedbackFragment : BaseFragment(R.xml.prefs_feedback, R.string.screen_feedback) {
    private val pickSound = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = requireContext().applicationContext
        background({
            runCatching {
                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                if (bytes.size > 1024 * 1024) return@runCatching false
                java.io.File(ctx.filesDir, "custom_sound").writeBytes(bytes)
                true
            }
        }) { result ->
            val ok = result.getOrNull() == true
            if (result.getOrNull() == false) { toast(R.string.sound_too_big); return@background }
            if (ok) { app.prefs.putString(K.SOUND_MODE, "custom"); preferenceScreen.findPreference<androidx.preference.ListPreference>(K.SOUND_MODE)?.value = "custom"; toast(R.string.sound_saved) } else toast(R.string.import_failed)
        }
    }

    override fun setup() { click("sound_pick") { pickSound.launch(arrayOf("audio/*")) } }
}

class ClipboardFragment : BaseFragment(R.xml.prefs_clipboard, R.string.screen_clipboard) {
    override fun setup() {
        findPreference<Preference>("clipboard_max")?.setOnPreferenceChangeListener { _, _ -> view?.post { app.clipboard.enforceLimit() }; true }
        click("clipboard_clear") { app.clipboard.clear(includePinned = false); toast(R.string.cleared) }
    }
}

/** The snippets: a shortcut and the text it turns into, one row each, with a dialog to make, change or delete them. */
class SnippetsFragment : BaseFragment(R.xml.prefs_snippets, R.string.screen_snippets) {
    override fun setup() {
        click("snippet_add") { edit(null) }
        rebuild()
    }

    private fun rebuild() {
        val ctx = requireContext()
        val category = findPreference<androidx.preference.PreferenceCategory>("snippet_list") ?: return
        category.removeAll()
        val list = app.prefs.snippets
        if (list.isEmpty()) category.addPreference(Preference(ctx).apply { title = getString(R.string.snippet_none); isEnabled = false; isIconSpaceReserved = false })
        for (s in list) category.addPreference(Preference(ctx).apply {
            title = s.key; summary = s.text; isIconSpaceReserved = false
            setOnPreferenceClickListener { edit(s); true }
        })
    }

    private fun save(list: List<com.metrowin.keyboard.engine.Snippet>) { app.prefs.snippets = list; rebuild() }

    /** [old] null: a new snippet. The dialog stays open until what is written is valid. */
    private fun edit(old: com.metrowin.keyboard.engine.Snippet?) {
        val ctx = requireContext()
        val pad = ctx.dp(24f).toInt()
        val key = EditText(ctx).apply {
            hint = getString(R.string.snippet_key_hint); setSingleLine(); setText(old?.key.orEmpty())
            filters = arrayOf(android.text.InputFilter.LengthFilter(com.metrowin.keyboard.engine.Snippets.MAX_KEY))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        val text = EditText(ctx).apply {
            hint = getString(R.string.snippet_text_hint); minLines = 3; gravity = android.view.Gravity.TOP; setText(old?.text.orEmpty())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val box = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(pad, ctx.dp(8f).toInt(), pad, 0); addView(key); addView(text) }
        val builder = MaterialAlertDialogBuilder(ctx).setTitle(if (old == null) R.string.snippet_dialog_new else R.string.snippet_dialog_edit).setView(box)
            .setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null)
        if (old != null) builder.setNeutralButton(R.string.snippet_delete) { _, _ -> save(app.prefs.snippets.filter { it.key != old.key }) }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val k = key.text.toString().trim(); val t = text.text.toString().trim()
                val others = app.prefs.snippets.filter { it.key != old?.key }
                when {
                    !com.metrowin.keyboard.engine.Snippets.validKey(k) -> toast(R.string.snippet_bad_key)
                    !com.metrowin.keyboard.engine.Snippets.validText(t) -> toast(R.string.snippet_bad_text)
                    others.any { it.key.equals(k, ignoreCase = true) } -> toast(R.string.snippet_duplicate)
                    old == null && others.size >= com.metrowin.keyboard.engine.Snippets.MAX_SNIPPETS -> toast(R.string.snippet_too_many)
                    else -> {
                        val now = com.metrowin.keyboard.engine.Snippet(k, t)
                        save(if (old == null) others + now else app.prefs.snippets.map { if (it.key == old.key) now else it })
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }
}

class VoiceFragment : BaseFragment(R.xml.prefs_voice, R.string.screen_voice) {
    private val ask = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    override fun setup() { click("voice_permission") { ask.launch(Manifest.permission.RECORD_AUDIO) } }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        val ok = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        findPreference<Preference>("voice_permission")?.summary = if (ok) getString(R.string.setup_enabled) else getString(R.string.pref_voice_permission_summary)
    }
}

class AboutFragment : BaseFragment(R.xml.prefs_about, R.string.screen_about) {
    override fun setup() {
        val v = runCatching { requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName }.getOrNull().orEmpty()
        findPreference<Preference>("about_version")?.summary = v
    }
}
