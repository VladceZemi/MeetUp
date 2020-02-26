package cz.brno.mendelu.meetup.functionclasses

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import cz.brno.mendelu.meetup.R
import java.util.regex.Matcher
import java.util.regex.Pattern

class DataProcessing(context: Context) {

    private var context: Context = context

    fun setErrorIfEmpty(editTextInput:EditText): Boolean {
        if (editTextInput.text.toString().isEmpty()){
            editTextInput.setError(context.getString(R.string.field_empty))
            return false
        }
        return true
    }

    fun setErrorIfBadEmail(editTextInput:EditText): Boolean{
        if (setErrorIfEmpty(editTextInput)) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(editTextInput.text.toString()).matches()) {
                return true
            } else {
                editTextInput.setError(context.getString(R.string.field_bad_email))
                return false
            }
        }
        return false
    }

    fun setErrorIfBadPhone(editTextInput:EditText): Boolean{
        if (setErrorIfEmpty(editTextInput)) {
            if (android.util.Patterns.PHONE.matcher(editTextInput.text.toString()).matches()) {
                return true
            } else {
                editTextInput.setError(context.getString(R.string.field_bad_phone))
                return false
            }
        }
        return false
    }

    fun setErrorIfBadPassword(editTextInput:EditText): Boolean{
        if (setErrorIfEmpty(editTextInput)) {
            if (editTextInput.text.length < 6) {
                editTextInput.setError(context.getString(R.string.field_bad_password))
                return false
            } else {
                return true
            }
        }
        return false
    }

    fun setErrorIfBadDate(textViewInput:TextView): Boolean{
        val m: Matcher = Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d\$").matcher(textViewInput.text)
        if (m.find()) {
            return true
        } else {
            textViewInput.error = "Nezadané datum!"
            return false
        }
    }

    fun setErrorIfBadTime(textViewInput:TextView): Boolean{
        val m: Matcher = Pattern.compile("^([0-1][0-9]|[2][0-3]):([0-5][0-9])\$").matcher(textViewInput.text)
        if (m.find()) {
            return true
        } else {
            textViewInput.error = "Nezadaný čas!"
            return false
        }
    }
}