# Lista de Compras

App Android simples, em Kotlin + Jetpack Compose, para uma lista de compras
compartilhada em tempo real entre dois celulares (você e sua esposa),
usando Firebase.

## O que o app faz

- Mostra uma lista numerada dos itens a comprar.
- Se estiver vazia, mostra só o número "1." e o campo para digitar o item.
- Sugere autocompletar com base em itens já cadastrados antes.
- Botão "+" adiciona o item digitado à lista.
- Ícone de lixeira apaga um item.
- Qualquer alteração feita em um celular aparece automaticamente no outro
  (Firestore em tempo real), sem precisar de login/senha (Firebase Anonymous Auth).

## Passo a passo para configurar o Firebase (obrigatório antes de rodar)

1. Acesse https://console.firebase.google.com e crie um projeto novo
   (ex.: "lista-compras-familia").

2. Dentro do projeto, adicione um app Android:
   - Nome do pacote: `br.com.yson.listacompras` (tem que ser exatamente esse,
     é o mesmo definido em `app/build.gradle.kts`).
   - Baixe o arquivo `google-services.json` gerado.
   - Substitua o arquivo `app/google-services.json` deste projeto pelo que
     você baixou (o que está aqui é só um placeholder e NÃO funciona).

3. No menu lateral do Firebase, vá em **Build > Authentication > Sign-in method**
   e ative o provedor **Anônimo**.

4. No menu lateral, vá em **Build > Firestore Database** e clique em
   **Criar banco de dados** (pode escolher o modo de produção; as regras
   abaixo cuidam da segurança). Escolha a região mais próxima (ex.: `southamerica-east1`).

5. Ainda no Firestore, vá na aba **Regras** e substitua pelo conteúdo abaixo,
   depois clique em **Publicar**:

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

   Isso libera leitura e escrita para qualquer usuário autenticado (mesmo
   anonimamente) — como só vocês dois vão instalar o app, é suficiente e
   mantém a simplicidade pedida.

6. Abra a pasta `ListaCompras` no Android Studio (versão mais recente),
   deixe o Gradle sincronizar, e rode o app nos dois celulares (ou em um
   celular e um emulador para testar).

7. Repita a instalação do mesmo app (com o mesmo `google-services.json`)
   no celular da sua esposa. Como os dois apontam para o mesmo projeto
   Firebase, a lista já aparece sincronizada nos dois.

## Estrutura do projeto

```
app/src/main/java/br/com/yson/listacompras/
├── MainActivity.kt              # Tela única (Compose): lista, campo, botão +, lixeira
├── ShoppingListViewModel.kt     # Login anônimo + escuta em tempo real do Firestore
└── ShoppingItem.kt              # Modelo de dados de um item da lista
```

## Coleções no Firestore

- `lista_compras`: os itens atuais da lista (nome, ordem).
- `itens_conhecidos`: nomes já digitados alguma vez, usados só para
  alimentar o autocompletar.
