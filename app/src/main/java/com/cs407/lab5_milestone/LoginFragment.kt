package com.cs407.lab5_milestone

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView
    private lateinit var sharedPrefFile: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        errorTextView = view.findViewById(R.id.errorTextView)
        sharedPrefFile = getString(R.string.userPasswdKV)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                handleLogin(username, password)
            } else {
                Toast.makeText(requireContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show()
            }
        }
        return view;
    }

    private fun handleLogin(username: String, password: String) {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        val hashedPassword = hashPassword(password)
        val db = NoteDatabase.getDatabase(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            val userDao = db.userDao()
            val existingUser = userDao.getUserByUsername(username)

            if (existingUser != null) {
                if (existingUser.passwordHash == hashedPassword) {
                    // Store the current user in SharedPreferences
                    with(sharedPreferences.edit()) {
                        putString("current_user", username)
                        apply()
                    }

                    withContext(Dispatchers.Main) {
                        navigateToNoteList(username, existingUser.userId)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.text = getString(R.string.fail_login)
                    }
                }
            } else {
                val userId = userDao.insertUser(User(username = username, passwordHash = hashedPassword)).toInt()

                with(sharedPreferences.edit()) {
                    putString("current_user", username)
                    apply()
                }

                withContext(Dispatchers.Main) {
                    navigateToNoteList(username, userId)
                }
            }
        }
    }


    private fun navigateToNoteList(username: String, userId: Int) {
        val action = LoginFragmentDirections.actionLoginFragmentToNoteListFragment(
            userId = userId,
            username = username
        )
        findNavController().navigate(action)
    }



    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

}