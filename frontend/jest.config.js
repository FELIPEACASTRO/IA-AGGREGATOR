const nextJest = require('next/jest');

const createJestConfig = nextJest({ dir: './' });

/** @type {import('jest').Config} */
const config = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testPathIgnorePatterns: ['<rootDir>/e2e/'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^react-markdown$': '<rootDir>/test/mocks/react-markdown.tsx',
    '^remark-gfm$': '<rootDir>/test/mocks/remark-gfm.ts',
    '^rehype-highlight$': '<rootDir>/test/mocks/rehype-highlight.ts',
  },
};

module.exports = createJestConfig(config);
