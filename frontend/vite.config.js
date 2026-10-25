import { readdir, unlink } from 'node:fs/promises'
import { resolve } from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

function removeStaleBuildAssets() {
  let generatedAssets = new Set()

  return {
    name: 'remove-stale-build-assets',
    apply: 'build',
    generateBundle(_options, bundle) {
      generatedAssets = new Set(
        Object.keys(bundle).filter((fileName) => fileName.startsWith('assets/')),
      )
    },
    async writeBundle(outputOptions) {
      const assetsDirectory = resolve(outputOptions.dir, 'assets')
      const files = await readdir(assetsDirectory)
      const staleHashedAsset = /^[\w.-]+-[\w-]{8,}\.(?:js|css)$/

      await Promise.all(
        files
          .filter(
            (fileName) =>
              staleHashedAsset.test(fileName) &&
              !generatedAssets.has(`assets/${fileName}`),
          )
          .map((fileName) => unlink(resolve(assetsDirectory, fileName))),
      )
    },
  }
}

export default defineConfig({
  plugins: [react(), removeStaleBuildAssets()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: false,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/js': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/css': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/img': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/styles': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/webjars': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
    },
  },
})
