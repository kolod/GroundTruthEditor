package io.github.kolod

import com.formdev.flatlaf.FlatLightLaf
import com.jcabi.manifests.Manifests
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.ItemEvent.*
import java.io.File
import java.util.*
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.JFileChooser.*


class GroundTruthEditor : JFrame() {
	enum class FinishedState {UNFINISHED, ANY}

	private val logger = LoggerFactory.getLogger(GroundTruthEditor::class.java)
	private val bundle = ResourceBundle.getBundle("i18n/GroundTruthEditor")
	private val prefs = Preferences.userNodeForPackage(GroundTruthEditor::class.java)
	private var id = 1
	private var directory :File? = null
		set(value) {
			if (value != null) {
				field = value
				prefs.put("directory", value.absolutePath)
				id = 1
				next(FinishedState.ANY)
			}
		}

	private val imageView                 = JLabel()
	private val imageViewScroll           = JScrollPane(imageView)
	private val textView                  = JEditorPane()
	private val textViewScroll            = JScrollPane(textView)

	private val renumberButton            = JButton()
	private val removeDuplicatesButton    = JButton()
	private val uncheckAllButton          = JButton()
	private val browseButton              = JButton()

	private val previousButton            = JButton()
	private val previousUnfinishedButton  = JButton()
	private val doneButton                = JToggleButton()
	private val doneAndNextButton         = JButton()
	private val nextButton                = JButton()
	private val nextUnfinishedButton      = JButton()
	private val deleteButton              = JButton()

	private fun loadIcons(name :String, extension :String = "png") :List<Image> {
		val toolkit = Toolkit.getDefaultToolkit()
		return listOf(16, 24, 32, 48, 64, 72, 96, 128, 256).map{ size ->
			"$name$size.$extension"
		}.mapNotNull{ path ->
			javaClass.classLoader.getResource(path)
		}.mapNotNull{ url ->
			try {
				toolkit.createImage(url)
			} catch (ex: Exception) {
				logger.warn(ex.message, ex)
				null
			}
		}
	}

	private fun translateUI() {
		with(bundle) {
			title                          = getString("title") + " " + Manifests.read("Build-Date")
			renumberButton.text            = getString("renumber_button")
			removeDuplicatesButton.text    = getString("remove_duplicates_button")
			uncheckAllButton.text          = getString("uncheck_all_button")
			browseButton.text              = getString("browse_button")
			previousButton.text            = getString("previous_button")
			previousUnfinishedButton.text  = getString("previous_unfinished_button")
			doneButton.text                = getString("done_button")
			doneAndNextButton.text         = getString("done_and_next_button")
			nextButton.text                = getString("next_button")
			nextUnfinishedButton.text      = getString("next_unfinished_button")
			deleteButton.text              = getString("delete_button")

			doneButton.addItemListener { event ->
				doneButton.text = when (event.stateChange) {
					SELECTED   -> getString("undone_button")
					DESELECTED -> getString("done_button")
					else -> ""
				}
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
	 * content of this method is always regenerated by the Form Editor.
	 */
	private fun initComponents() {
		preferredSize = Dimension(1200, 600)
		defaultCloseOperation = EXIT_ON_CLOSE
		iconImages = loadIcons("icon/")

		translateUI()

		val splitter = JSplitPane(JSplitPane.VERTICAL_SPLIT, imageViewScroll, textViewScroll)

		val header = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
			add(deleteButton)
			add(renumberButton)
			add(removeDuplicatesButton)
			add(uncheckAllButton)
			add(browseButton)
		}

		val footer = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
			add(previousButton)
			add(previousUnfinishedButton)
			add(doneButton)
			add(doneAndNextButton)
			add(nextButton)
			add(nextUnfinishedButton)
		}

		with (contentPane) {
			add(header, BorderLayout.NORTH)
			add(splitter, BorderLayout.CENTER)
			add(footer, BorderLayout.SOUTH)
		}

		pack()
		setLocationRelativeTo(null)
		splitter.dividerLocation = splitter.height / 2
	}

	private fun stringID() :String = id.toString().padStart(4, '0')

	private fun open(newId :Int, finished :FinishedState) :Boolean = try {
		id = newId
		val idStr = stringID()
		val pngFile = File(directory, "$idStr.png")
		val txtCheckedFile = File(directory, "$idStr.gt.txt")
		val txtUncheckedFile = File(directory, "$idStr.txt")

		when (finished) {
			FinishedState.UNFINISHED ->
				if (txtUncheckedFile.exists()) {
					doneButton.isSelected = false
					textView.setTextColoredByLang(txtUncheckedFile.readText())
					imageView.icon = ImageIcon(ImageIO.read(pngFile))
					true
				} else false

			FinishedState.ANY ->
				if (txtCheckedFile.exists()) {
					doneButton.isSelected = true
					textView.setTextColoredByLang(txtCheckedFile.readText())
					imageView.icon = ImageIcon(ImageIO.read(pngFile))
					true
				} else if (txtUncheckedFile.exists()) {
					doneButton.isSelected = false
					textView.setTextColoredByLang(txtUncheckedFile.readText())
					imageView.icon = ImageIcon(ImageIO.read(pngFile))
					true
				} else false
			}
		} catch (ex :Exception) {
			logger.error(ex.message, ex)
			false
		}

	private fun save(checked :Boolean) = try {
		val idStr = stringID()
		val txtCheckedFile = File(directory, "$idStr.gt.txt")
		val txtUncheckedFile = File(directory, "$idStr.txt")
		if (checked) {
			logger.debug("Save: ${txtCheckedFile.absolutePath}")
			txtCheckedFile.writeText(textView.getPlainText())
			txtUncheckedFile.delete()
		} else {
			logger.debug("Save: ${txtUncheckedFile.absolutePath}")
			txtUncheckedFile.writeText(textView.getPlainText())
			txtCheckedFile.delete()
		}
	} catch (ex :Exception) {
		logger.error(ex.message, ex)
	}

	fun next(finished :FinishedState) {
		for (i in id + 1 .. 9999) {
			if (open(i, finished)) break
		}
	}

	fun previous(finished :FinishedState) {
		for (i in id - 1 downTo 1) {
			if (open(i, finished)) break
		}
	}

	/**
	 * Creates new form TestTrainer
	 */
	init {
		logger.info("Application started")
		initComponents()

		prefs.get("directory", null)?.let {
			directory = File(it)
		}

		//textViewScroll.

		removeDuplicatesButton.addActionListener {
			directory?.deleteDuplicatesWithCompanions(".*\\.png".toRegex())?.forEach { file ->
				logger.debug("Removed: ${file.absolutePath}")
			}
		}

		renumberButton.addActionListener {
			directory?.renumberWithCompanions(".*\\.png") { current, total ->
				true
			}
		}

		uncheckAllButton.addActionListener {
			directory?.list { _, filename ->
				filename.endsWith(".gt.txt")
			}?.mapNotNull { name ->
				File(directory, name).renameTo( File(directory, name.split(".").first() + ".txt"))
			}
		}

		deleteButton.addActionListener {
			File(directory, "${stringID()}.png").getCompanions().forEach{ it.delete() }
			next(FinishedState.ANY)
		}

		browseButton.addActionListener {
			val dialog = JFileChooser()
			dialog.fileSelectionMode = DIRECTORIES_ONLY
			dialog.isMultiSelectionEnabled = false
			dialog.selectedFile = directory
			if (dialog.showOpenDialog(this) == APPROVE_OPTION) directory = dialog.selectedFile
			directory?.absolutePath?.let { path ->
				prefs.put("directory", path)
				logger.info("Directory: $path")
				next(FinishedState.ANY)
			}
		}

		doneButton.addActionListener { event ->
			(event.source as? JToggleButton)?.let{ button ->
				save(button.model.isSelected)
			}
		}

		doneAndNextButton.addActionListener {
			save(true)
			next(FinishedState.UNFINISHED)
		}

		nextButton.addActionListener {
			next(FinishedState.ANY)
		}

		nextUnfinishedButton.addActionListener {
			next(FinishedState.UNFINISHED)
		}

		previousButton.addActionListener{
			previous(FinishedState.ANY)
		}

		previousUnfinishedButton.addActionListener{
			previous(FinishedState.UNFINISHED)
		}
	}

	companion object {
		/**
		 * @param args the command line arguments
		 */
		@JvmStatic
		fun main(args: Array<String>) {
			FlatLightLaf.setup()
			UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(14f))
			SwingUtilities.invokeLater { GroundTruthEditor().isVisible = true }
		}
	}
}
