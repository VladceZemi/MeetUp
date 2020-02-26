package cz.brno.mendelu.meetup.activities.friends

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.activities.MainActivity
import kotlinx.android.synthetic.main.dialog_add_contact.view.*


class AddContactDialog: AppCompatDialogFragment() {

    private lateinit var listener: AddContactDialogInterface

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)

        val inflater = activity!!.layoutInflater
        val view: View = inflater.inflate(R.layout.dialog_add_contact, null)

        builder.setView(view)
            .setTitle(getString(R.string.add_con_title))
            .setNegativeButton("Zrušit", DialogInterface.OnClickListener { _, _ ->  })
            .setPositiveButton("Přidat", DialogInterface.OnClickListener { _, _ ->
                var email: String = view.contactEmailInput.text.toString()
                listener.addContact(email)
            })
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as MainActivity).currentFragment as AddContactDialogInterface
    }

    interface AddContactDialogInterface {
        fun addContact(email: String)
    }
}