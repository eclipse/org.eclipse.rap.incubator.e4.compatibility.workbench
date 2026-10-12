# Port RAP 4.7 + E4: estado de continuación

Última actualización: 2026-10-12.

## Objetivo

- Ejecutar el workbench E4 sobre Eclipse RAP 4.7.
- Conservar la API de compatibilidad Eclipse 3.x sobre el modelo E4.
- Mantener las implementaciones web de RAP 4.7, especialmente `org.eclipse.rap.ui.*` y `PartPane`.
- No reintroducir el stack clásico RCP como sustituto de `Workbench`, `WorkbenchPage`, `WorkbenchWindow`, `Perspective` y las referencias modeladas E4.

## Estado validado

El reactor completo compila y empaqueta correctamente con la target del repositorio:

```powershell
mvn --no-transfer-progress -f releng/org.eclipse.rap.e4.compatibility.workbench.build/pom.xml -DskipTests verify
```

Resultado: `BUILD SUCCESS` en los cinco proyectos:

1. build padre;
2. target definition;
3. `org.eclipse.rap.ui.workbench`;
4. `org.eclipse.rap.e4clw.feature`;
5. repositorio p2.

Log de la última ejecución: `%TEMP%\rap-workbench-471-format-cleanup.log`.

Artefactos generados:

- `bundles/org.eclipse.rap.ui.workbench/target/org.eclipse.rap.ui.workbench-4.7.1-SNAPSHOT.jar`
- `releng/org.eclipse.rap.e4.compatibility.workbench.build/repository/target/org.eclipse.rap.e4clw-repository-4.7.1-SNAPSHOT.zip`

Las versiones están alineadas en `4.7.1-SNAPSHOT` / `4.7.1.qualifier`. No quedan imports activos de `javax.inject` ni `javax.annotation`.

## Decisiones de port importantes

- `PartPane` conserva la implementación completa de RAP 4.7 y actúa también como fachada para las referencias E4. La activación termina en `WorkbenchPage.activate`, que delega en `EPartService`.
- `PartSite`, `EditorSite`, `ViewSite` y `WorkbenchPartReference` mantienen el puente de compatibilidad 3.x/E4.
- Se conserva el núcleo E4 de `Perspective`, `Workbench`, `WorkbenchPage` y `WorkbenchWindow`.
- `PagePartSelectionTracker` usa de nuevo la implementación funcional de RAP 4.7; el stub que impedía el seguimiento de selección fue sustituido.
- Se retiró el ruido de espacios/tabuladores de los archivos modificados. La recompilación no incremental resultante detectó y corrigió tres dependencias clásicas residuales: `PartStack`, `ViewPane` y el estado `reported` de `JobInfo`.
- `ShowViewHandler` combina el diálogo RAP con la apertura modelada E4: el diálogo devuelve descriptores 3.x y éstos se resuelven contra `MPartDescriptor`/`EPartService`.
- Los mensajes NLS se obtienen mediante `get()` para mantener el ámbito de sesión RAP. Las coincidencias restantes del patrón estático están comentadas o dentro de las propias clases NLS.
- Las APIs exclusivamente de escritorio (taskbar del SO y heap-status global) se omiten en RAP. El progreso normal dentro de la UI permanece activo.
- La importación/exportación de perspectivas conserva los tópicos E4 sin depender de los wizards de preferencias de escritorio que no existen en RAP.
- `ModeledPageLayout` conserva los métodos antiguos de fast views como compatibilidad y cumple el contrato 4.7 de editor onboarding mediante tags del modelo.
- Se eliminó únicamente `PreferencesSettingsTransfer`: no estaba registrada, no existe en RAP y dependía de `WorkbenchSettingsTransfer` del entorno IDE/RCP.
- Las clases clásicas temporales de stacks, panes y fast views que no existen ni en el port E4 3.9 ni en Platform/E4 fueron retiradas tras clasificar sus consumidores; no se reemplazó el núcleo E4.

## Qué falta validar en ejecución

La compilación y el ensamblado p2 están resueltos, pero todavía conviene hacer pruebas funcionales con una aplicación RAP real:

- abrir/cerrar vistas y editores y restaurar el modelo;
- cambiar, guardar, reiniciar y eliminar perspectivas;
- selección y post-selección entre partes;
- menús, comandos, key bindings y action sets;
- operaciones de guardado y progreso en sesiones concurrentes;
- intro/branding y aislamiento de textos NLS entre sesiones;
- instalación del zip p2 en la target final ETS5.

Las advertencias Tycho actuales son de configuración (`archiveSite` desconocido, repositorios p2 con varios IDs y dependencias system no mapeadas); no impiden construir ni empaquetar.

## Referencias locales usadas

- RAP 4.7 original: `%TEMP%\eclipse-rap-4.7.0-R\bundles\org.eclipse.rap.ui.workbench`
- Rama RAP/E4 3.9 extraída: `%TEMP%\rap-workbench-39-source\bundles\org.eclipse.rap.ui.workbench`
- Eclipse Platform R4_40: `%TEMP%\rap-workbench-port-47\new\bundles\org.eclipse.ui.workbench`
- Target preparada por el usuario: `C:\E2509\workspace_EUD5\_git\open-libraries\Project_OPEN-Libraries\Development_target-platforms\ETS5_E2606_RCP_LOCAL.target`

La rama actual es `4.7`. No se ha creado commit.
