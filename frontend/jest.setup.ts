import '@testing-library/jest-dom';

Object.defineProperty(window.HTMLElement.prototype, 'scrollIntoView', {
	writable: true,
	value: jest.fn(),
});
