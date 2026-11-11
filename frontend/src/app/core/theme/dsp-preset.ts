import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';

/**
 * PrimeNG preset for the Digital Support Portal.
 * Source of truth: the DSP v4 design PDF; raw values live in src/styles/tokens.css.
 */
export const DspPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#f3f7fe',
      100: '#e8effc',
      200: '#c9daf6',
      300: '#9dbbee',
      400: '#5f8fe0',
      500: '#2a68cf',
      600: '#0047ba',
      700: '#00399a',
      800: '#002b70',
      900: '#001f52',
      950: '#001538',
    },
    formField: {
      borderRadius: '12px',
      paddingX: '14px',
      paddingY: '12px',
      focusRing: { width: '3px', style: 'solid', color: 'rgb(0 71 186 / 0.25)', offset: '0' },
    },
    focusRing: { width: '2px', style: 'solid', color: '#0047ba', offset: '2px' },
    colorScheme: {
      light: {
        primary: {
          color: '#0047ba',
          contrastColor: '#ffffff',
          hoverColor: '#00399a',
          activeColor: '#002b70',
        },
        highlight: { background: '#e8effc', focusBackground: '#c9daf6', color: '#00399a', focusColor: '#00399a' },
        surface: {
          0: '#ffffff',
          50: '#f7f9fd',
          100: '#f3f6fc',
          200: '#e6ebf3',
          300: '#d5ddea',
          400: '#9aa5b8',
          500: '#5e6a82',
          600: '#44506a',
          700: '#2a3650',
          800: '#1a2640',
          900: '#0f1b33',
          950: '#0a1224',
        },
        formField: {
          background: '#ffffff',
          borderColor: '#d5ddea',
          hoverBorderColor: '#9dbbee',
          focusBorderColor: '#0047ba',
          invalidBorderColor: '#b42318',
          color: '#0f1b33',
          placeholderColor: '#5e6a82',
          invalidPlaceholderColor: '#b42318',
        },
        text: { color: '#0f1b33', hoverColor: '#0f1b33', mutedColor: '#44506a', hoverMutedColor: '#0f1b33' },
        content: { background: '#ffffff', hoverBackground: '#f3f6fc', borderColor: '#e6ebf3', color: '#0f1b33', hoverColor: '#0f1b33' },
      },
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '12px',
        paddingX: '18px',
        paddingY: '12px',
        sm: { paddingX: '14px', paddingY: '10px' },
        lg: { paddingX: '22px', paddingY: '14px' },
        label: { fontWeight: '700' },
      },
    },
    card: {
      root: { borderRadius: '20px', shadow: '0 2px 4px rgb(15 27 51 / 0.04), 0 8px 24px rgb(15 27 51 / 0.06)' },
    },
    dialog: {
      root: { borderRadius: '24px', shadow: '0 8px 16px rgb(15 27 51 / 0.08), 0 24px 56px rgb(15 27 51 / 0.14)' },
    },
    toast: {
      root: { borderRadius: '12px' },
    },
    tag: {
      root: { borderRadius: '999px', fontSize: '12px', fontWeight: '700' },
    },
  },
});
