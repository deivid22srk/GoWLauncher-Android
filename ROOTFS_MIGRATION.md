# 🔄 Migração para Orion RootFS

## O que mudou?

### ✅ Antes (Sistema Antigo)
- Assets embutidos no APK (~500MB)
- Download automático durante build
- APK muito pesado
- Atualizações requerem novo APK

### ✅ Agora (Sistema Novo com RootFS)
- APK leve (~50-100MB)
- Assets separados em pacote .orfs
- Usuário baixa e importa manualmente
- Atualizações independentes do APK

---

## 🏗️ Arquitetura Nova

```
GoWLauncher APK (leve)
    ↓
Primeira execução
    ↓
Usuário baixa Orion RootFS (.orfs)
    ↓
Importa no app
    ↓
Sistema instalado e pronto
```

---

## 📦 Componentes

### 1. Orion-RootFs Repository
**URL**: https://github.com/deivid22srk/Orion-RootFs

**Conteúdo:**
- Scripts de build
- Workflow GitHub Actions
- Metadata de assets
- Documentação

**Não contém** (muito pesado para Git):
- imagefs.txz (baixado durante build)
- proton-9.0-arm64ec.txz (baixado durante build)

**Contém** (assets médios/pequenos):
- graphics_driver/*.tzst
- dxwrapper/*.tzst
- wincomponents/*.tzst
- Outros componentes

### 2. GoWLauncher-Android (App)

**Modificações:**
- `RootFsImportActivity.java` → Nova activity de importação
- `ImageFsInstaller.java` → Métodos para instalar de .orfs
- `HomeFragment.java` → Opções de importação
- `build.gradle` → Removido download de assets pesados
- `AndroidManifest.xml` → Registrada nova activity

**Assets Removidos:**
- ❌ imagefs.txz (não mais embutido)
- ❌ proton-9.0-arm64ec.txz (não mais embutido)

**Assets Mantidos:**
- ✅ graphics_driver/ (ainda usados como fallback)
- ✅ Outros assets pequenos (<1MB cada)

---

## 🔧 Como Funciona

### Build do RootFS (GitHub Actions)

```yaml
Workflow Trigger (push/tag)
    ↓
Download assets externos (GitLab)
    ├─ imagefs.txz (4 partes)
    └─ proton-9.0-arm64ec.txz
    ↓
Copiar assets do repositório
    ├─ graphics_driver/
    ├─ dxwrapper/
    └─ outros componentes
    ↓
Organizar estrutura
    ↓
Criar metadata.json
    ↓
Compactar tudo em .orfs (tar.zst)
    ↓
Gerar checksum SHA256
    ↓
Criar GitHub Release
```

### Importação no App

```java
Usuário seleciona .orfs
    ↓
ImageFsInstaller.installFromRootFs()
    ├─ Extrair .orfs para temp/
    ├─ Validar estrutura
    ├─ Ler metadata.json
    ├─ Extrair imagefs.txz → /data/data/app/files/imagefs/
    ├─ Extrair proton.txz → imagefs/opt/
    ├─ Copiar drivers → files/contents/adrenotools/
    ├─ Copiar outros componentes
    ├─ Criar .img_version
    └─ Limpar temp/
    ↓
Sistema pronto!
```

---

## 📊 Comparação de Tamanhos

| Componente | Antes | Depois |
|------------|-------|--------|
| APK (App) | ~500MB | ~50MB |
| Download inicial | Automático | Manual (~500MB .orfs) |
| Espaço em disco | ~3GB | ~3GB |
| Tempo de instalação | Durante build | Durante importação |
| **Total download usuário** | **500MB** | **50MB + 500MB** |

### Por que isso é melhor?

1. **APK pequeno**: Mais fácil de distribuir e atualizar
2. **Separação**: Assets pesados separados do código
3. **Flexibilidade**: Usuário pode usar RootFS customizado
4. **Updates**: Pode atualizar RootFS sem atualizar app
5. **Build rápido**: Desenvolvedores não precisam baixar 500MB a cada build

---

## 🚀 Workflow de Desenvolvimento

### Atualizar App (Código)
```bash
# Modificar código Java/Kotlin
git add .
git commit -m "Feature X"
git push

# Build é rápido (sem download de assets)
```

### Atualizar RootFS (Sistema)
```bash
# Modificar assets no Orion-RootFs
cd Orion-RootFs
git add sources/
git commit -m "Update driver to v2"
git push

# GitHub Actions compila automaticamente
# Release criado em ~10-15 minutos
```

### Release Completo
1. Atualizar app: Novo APK com código
2. Atualizar RootFS: Novo .orfs com sistema
3. Usuário:
   - Instala novo APK (~50MB)
   - Importa novo RootFS (~500MB) - opcional

---

## ⚠️ Notas Importantes

### Para Usuários Existentes

Se você já tem o GoWLauncher instalado com sistema antigo:
- Seus jogos e saves estão em `/home/xuser-X/`
- A importação do RootFS **preserva** a pasta `/home`
- Você pode atualizar sem perder dados

### Para Novos Usuários

- Primeira instalação requer:
  1. Instalar APK (~50MB)
  2. Baixar RootFS (~500MB)
  3. Importar no app (~3-5 min)
- Total: ~550MB download + 3GB disco

### Vantagens vs Sistema Antigo

| Aspecto | Antigo | Novo |
|---------|--------|------|
| Download APK | 500MB | 50MB ✅ |
| Instalação app | 1 passo | 2 passos |
| Atualizações | Baixar APK inteiro | Apenas código ou RootFS ✅ |
| Customização | Limitada | RootFS customizável ✅ |
| Build speed | Lento (~5min) | Rápido (~1min) ✅ |

---

## 🔗 Links Úteis

- [Orion-RootFs Releases](https://github.com/deivid22srk/Orion-RootFs/releases)
- [GoWLauncher Repository](https://github.com/deivid22srk/GoWLauncher-Android)
- [Reportar Issue - RootFS](https://github.com/deivid22srk/Orion-RootFs/issues)
- [Reportar Issue - App](https://github.com/deivid22srk/GoWLauncher-Android/issues)
