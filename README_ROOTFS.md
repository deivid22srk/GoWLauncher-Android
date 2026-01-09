# 🎮 GoWLauncher - Sistema Modular com Orion RootFS

## 🆕 Novidades - Sistema Modular

A partir desta versão, o GoWLauncher usa um **sistema modular** com RootFS externo:

### O que mudou?

**Antes:**
- ❌ APK pesado (~500MB)
- ❌ Assets embutidos no APK
- ❌ Build lento (download durante compilação)

**Agora:**
- ✅ APK leve (~50-100MB)
- ✅ RootFS separado (baixado pelo usuário)
- ✅ Build rápido
- ✅ Atualizações independentes

---

## 📥 Como Instalar (Primeira Vez)

### Passo 1: Instalar o APK
```
1. Baixe o GoWLauncher.apk
2. Instale no dispositivo
3. Abra o app
```

### Passo 2: Baixar Orion RootFS
```
1. Acesse: https://github.com/deivid22srk/Orion-RootFs/releases/latest
2. Baixe: orion-rootfs-v1.0.orfs (~500MB)
```

### Passo 3: Importar no App
```
1. Na tela inicial, clique "Importar RootFS"
2. Selecione o arquivo .orfs baixado
3. Aguarde 3-5 minutos
4. Pronto! Sistema instalado
```

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────┐
│  GoWLauncher APK (~50MB)            │
│  ├─ Código Java/Kotlin              │
│  ├─ Recursos UI (layouts, drawables)│
│  └─ Assets pequenos (<1MB)          │
└─────────────────────────────────────┘
              ↓ importa
┌─────────────────────────────────────┐
│  Orion RootFS (.orfs ~500MB)        │
│  ├─ ImageFS (sistema Linux)         │
│  ├─ Proton 9.0 ARM64EC (Wine)       │
│  ├─ Drivers gráficos (Turnip)       │
│  ├─ DXVK/VKD3D (DirectX wrappers)   │
│  └─ Componentes Wine                │
└─────────────────────────────────────┘
              ↓ extrai para
┌─────────────────────────────────────┐
│  /data/data/app/files/imagefs/      │
│  └─ Sistema completo (~3GB)         │
└─────────────────────────────────────┘
```

---

## 🔧 Para Desenvolvedores

### Build do APK (Rápido)
```bash
# Clone o repositório
git clone https://github.com/deivid22srk/GoWLauncher-Android.git
cd GoWLauncher-Android

# Build (SEM download de assets pesados)
./gradlew assembleDebug

# APK gerado em: app/build/outputs/apk/debug/
```

### Build do RootFS (Separado)
```bash
# Clone o repositório RootFS
git clone https://github.com/deivid22srk/Orion-RootFs.git
cd Orion-RootFs

# Download e compile
bash scripts/download.sh
bash scripts/compile.sh

# Arquivo gerado em: output/orion-rootfs-v1.0.orfs
```

### Workflow Automático
- Push no `Orion-RootFs` → GitHub Actions compila automaticamente
- Push no `GoWLauncher-Android` → Build normal do APK
- Tag no `Orion-RootFs` → Cria release com .orfs

---

## 📂 Novos Arquivos

### Java Classes
- `RootFsImportActivity.java` → Activity de importação
- `ImageFsInstaller.java` → Métodos para instalar de .orfs (modificado)
- `HomeFragment.java` → Opções de importação (modificado)

### Layouts
- `rootfs_import_activity.xml` → UI de importação

### Configs
- `build.gradle` → Removido download de assets pesados
- `AndroidManifest.xml` → Registrada RootFsImportActivity

### Docs
- `ROOTFS_MIGRATION.md` → Guia de migração
- `README_ROOTFS.md` → Este arquivo

---

## 🔗 Links

- **Orion RootFS**: https://github.com/deivid22srk/Orion-RootFs
- **Releases RootFS**: https://github.com/deivid22srk/Orion-RootFs/releases
- **GoWLauncher**: https://github.com/deivid22srk/GoWLauncher-Android

---

## ❓ FAQ

**P: Por que separar o RootFS?**
R: Para tornar o APK menor, facilitar atualizações e permitir customizações.

**P: Preciso baixar o RootFS toda vez?**
R: Não, apenas na primeira instalação ou ao atualizar o sistema.

**P: Posso usar RootFS customizado?**
R: Sim! Você pode compilar seu próprio .orfs com modificações.

**P: Meus saves são preservados?**
R: Sim, a pasta `/home` é sempre preservada durante atualizações.

**P: Como atualizo o RootFS?**
R: Baixe a nova versão e reimporte. Seus dados serão mantidos.

**P: O app funciona sem RootFS?**
R: Não, o RootFS é necessário para executar jogos Windows.

---

## 🐛 Correções desta Versão

### Fix: App fechando ao clicar em jogo
- **Problema**: Ao clicar em um jogo, o app fechava completamente
- **Causa**: `System.exit(0)` sendo chamado no callback de terminação
- **Solução**: Substituído por `finish()` para voltar à tela inicial
- **Arquivos**: `XServerDisplayActivity.java`, `XrActivity.java`

### Melhoria: Sistema Modular
- **Implementado**: Sistema de RootFS externo
- **Benefício**: APK 90% menor
- **Arquivos**: Vários (ver acima)

---

**Desenvolvido por: deivid22srk**
**Versão: 1.0.0-gow-rootfs**
