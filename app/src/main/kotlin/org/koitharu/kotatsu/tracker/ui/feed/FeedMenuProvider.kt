package org.koitharu.kotatsu.tracker.ui.feed

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.databinding.DialogClearFeedBinding

class FeedMenuProvider(
	private val snackbarHost: View,
	private val viewModel: FeedViewModel,
) : MenuProvider {

	private val context: Context
		get() = snackbarHost.context

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_show_updated)?.isChecked = viewModel.isHeaderEnabled.value
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_update -> {
			viewModel.update()
			true
		}

		R.id.action_show_updated -> {
			viewModel.setHeaderEnabled(!menuItem.isChecked)
			true
		}

		R.id.action_clear_feed -> {
			val binding = DialogClearFeedBinding.inflate(LayoutInflater.from(context))
			buildAlertDialog(context, isCentered = true) {
				setIcon(R.drawable.ic_clear_all)
				setTitle(R.string.clear_updates_feed)
				setMessage(R.string.text_clear_updates_feed_prompt)
				setNegativeButton(android.R.string.cancel, null)
				setView(binding.root)
				setPositiveButton(R.string.clear) { _, _ ->
					viewModel.clearFeed(
						readOnly = binding.checkboxReadOnly.isChecked,
						clearCounters = binding.checkboxCounters.isChecked,
					)
				}
			}.show()
			true
		}

		else -> false
	}
}
