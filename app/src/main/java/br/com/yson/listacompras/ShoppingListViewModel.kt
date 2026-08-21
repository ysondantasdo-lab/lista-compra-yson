package br.com.yson.listacompras

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Toda a lógica do app fica aqui: login anônimo, escuta em tempo real
 * da lista compartilhada e escuta dos itens já conhecidos (autocompletar).
 */
class ShoppingListViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val itensCollection = db.collection("lista_compras")
    private val itensConhecidosCollection = db.collection("itens_conhecidos")

    private val _itens = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val itens: StateFlow<List<ShoppingItem>> = _itens

    private val _itensConhecidos = MutableStateFlow<List<String>>(emptyList())
    val itensConhecidos: StateFlow<List<String>> = _itensConhecidos

    init {
        entrarAnonimamente()
    }

    private fun entrarAnonimamente() {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual == null) {
            auth.signInAnonymously().addOnCompleteListener { tarefa ->
                if (tarefa.isSuccessful) {
                    escutarMudancas()
                }
            }
        } else {
            escutarMudancas()
        }
    }

    private fun escutarMudancas() {
        // Lista de compras, sempre ordenada. Qualquer alteração feita em
        // qualquer um dos dois celulares chega aqui automaticamente.
        itensCollection.orderBy("ordem").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _itens.value = snapshot.documents.mapNotNull { documento ->
                    documento.toObject(ShoppingItem::class.java)?.copy(id = documento.id)
                }
            }
        }

        // Nomes já usados alguma vez, para sugerir no autocompletar.
        itensConhecidosCollection.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _itensConhecidos.value = snapshot.documents.mapNotNull { it.getString("nome") }
            }
        }
    }

    fun adicionarItem(nome: String) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) return

        val proximaOrdem = (_itens.value.maxOfOrNull { it.ordem } ?: 0L) + 1L

        val novoItem = hashMapOf(
            "nome" to nomeLimpo,
            "ordem" to proximaOrdem
        )
        itensCollection.add(novoItem)

        // Guarda o nome para sugerir da próxima vez (chave = nome em minúsculo,
        // pra não duplicar "Pão Francês" e "pão francês").
        itensConhecidosCollection.document(nomeLimpo.lowercase())
            .set(hashMapOf("nome" to nomeLimpo, "atualizadoEm" to FieldValue.serverTimestamp()))
    }

    fun removerItem(id: String) {
        itensCollection.document(id).delete()
    }
}
