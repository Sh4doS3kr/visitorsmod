# 🤝 Contribución

![Open Source Love](https://img.shields.io/badge/Open%20Source-%E2%9D%A4%EF%B8%8F-red?style=flat-square)
![Pull Requests Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square&logo=github)
![Java Style](https://img.shields.io/badge/code%20style-Google-blue?style=flat-square&logo=google)
![Discord](https://img.shields.io/badge/Discord-Dev%20Chat-5865F2?style=flat-square&logo=discord)
![CI Status](https://img.shields.io/badge/CI%2FCD-Pipeline%20Passing-success?style=flat-square&logo=github-actions)

¡Gracias por tu interés en colaborar con la arquitectura del **Visitors Mod**! Valoramos enormemente las contribuciones de la comunidad técnica para optimizar y escalar este proyecto.

---

## 🛠️ Flujo de Trabajo (Git Workflow)

Para mantener la integridad del código base, utilizamos un modelo de ramas estricto ("Feature Branch Workflow").

```mermaid
gitGraph
   commit id: "Master Stable"
   branch feature/nueva-funcionalidad
   checkout feature/nueva-funcionalidad
   commit id: "Init Logic"
   commit id: "Implement Fix"
   commit id: "Unit Tests"
   checkout main
   merge feature/nueva-funcionalidad tag: "PR Review"