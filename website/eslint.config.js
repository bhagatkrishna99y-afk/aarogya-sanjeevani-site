import js from "@eslint/js";
import babelParser from "@babel/eslint-parser";
import jsxA11y from "eslint-plugin-jsx-a11y";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";
import prettierConfig from "eslint-config-prettier";

// This project's actual compiler is TypeScript 7 (the new native port) --
// see README.md's "Tooling notes" section. typescript-eslint doesn't run
// against TS 7 yet (it hard-fails: see
// https://github.com/typescript-eslint/typescript-eslint/issues/10940),
// and swapping the whole project's compiler down to a TS 6-compat shim
// just to satisfy a linter isn't worth it. Babel's parser strips TS syntax
// for linting purposes without needing the `typescript` package at all --
// every rule enabled below (jsx-a11y, react-hooks, react-refresh) is purely
// AST-based and needs no type information, so this loses nothing we
// actually use. Real type errors are still caught by `tsc -b` (the build
// script) and the editor, which both talk to the real TS 7 compiler.
export default [
  {
    // `**/*.d.ts` (ambient globals, no runtime code) and the Supabase-generated
    // database types (deeply generic, type-only, no JSX/hooks/a11y concerns --
    // and a poor fit for Babel's type-stripper, which targets everyday
    // application code, not generated declaration-style type files) --
    // both are reviewed via `tsc -b`, not this linter.
    ignores: [
      "dist",
      "coverage",
      "playwright-report",
      "test-results",
      ".oxlintrc.json",
      "**/*.d.ts",
      "src/lib/database.types.ts",
    ],
  },
  {
    files: ["**/*.{js,jsx,ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2023,
      sourceType: "module",
      parser: babelParser,
      parserOptions: {
        requireConfigFile: false,
        babelOptions: {
          presets: [
            "@babel/preset-react",
            ["@babel/preset-typescript", { isTSX: true, allExtensions: true }],
          ],
        },
      },
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
      "jsx-a11y": jsxA11y,
    },
    rules: {
      ...js.configs.recommended.rules,
      ...reactHooks.configs.recommended.rules,
      ...jsxA11y.configs.recommended.rules,
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
      // Babel's parser doesn't patch eslint-scope with TypeScript-aware binding
      // analysis the way typescript-eslint does, so scope-based rules can't
      // tell a `type`/`interface` declaration (or `as const`, `export type`)
      // from an undefined value reference. `tsc -b` already enforces both of
      // these correctly, with full type information and no false positives --
      // this is the standard, documented tradeoff of linting TS via Babel
      // instead of typescript-eslint (see this file's header comment).
      "no-unused-vars": "off",
      "no-undef": "off",
    },
  },
  {
    // Node-context config files (this one, vite.config.ts, playwright.config.ts).
    files: ["*.config.{js,ts}"],
    languageOptions: { globals: globals.node },
  },
  prettierConfig,
];
