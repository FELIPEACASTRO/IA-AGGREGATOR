'use client';

import { ReactNode } from 'react';
import { Input, type InputProps } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/cn';

type FieldBaseProps = {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  required?: boolean;
  className?: string;
  fieldClassName?: string;
};

export type FieldProps = FieldBaseProps & InputProps;

export function Field({
  id,
  label,
  hint,
  error,
  required,
  className,
  fieldClassName,
  ...inputProps
}: FieldProps) {
  return (
    <div className={cn('space-y-1.5', className)}>
      <label htmlFor={id} className="text-[13px] font-medium text-[var(--muted-foreground)]">
        {label}
      </label>
      <Input id={id} error={error} className={fieldClassName} required={required} {...inputProps} />
      {(hint || error) && (
        <p className={cn('text-[12px]', error ? 'text-[var(--destructive)]' : 'text-[var(--subtle-foreground)]')}>
          {error || hint}
        </p>
      )}
    </div>
  );
}

type SelectFieldOption = { value: string; label: string };

type SelectFieldProps = FieldBaseProps & {
  value: string;
  onChange: (value: string) => void;
  options: SelectFieldOption[];
  icon?: ReactNode;
  disabled?: boolean;
};

export function SelectField({
  id,
  label,
  hint,
  error,
  required,
  className,
  fieldClassName,
  value,
  onChange,
  options,
  icon,
  disabled,
}: SelectFieldProps) {
  return (
    <div className={cn('space-y-1.5', className)}>
      <label htmlFor={id} className="text-[13px] font-medium text-[var(--muted-foreground)]">
        {label}
      </label>
      <div className="relative">
        {icon ? (
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]">
            {icon}
          </span>
        ) : null}
        <select
          id={id}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          required={required}
          disabled={disabled}
          className={cn(
            'h-10 w-full rounded-[var(--radius-md)] border border-[var(--input-border)] bg-[var(--input-bg)] px-3 text-[14px] text-[var(--foreground)] outline-none transition-colors focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--ring)] disabled:cursor-not-allowed disabled:opacity-50',
            icon ? 'pl-9' : '',
            error ? 'border-[var(--destructive)] focus:border-[var(--destructive)]' : '',
            fieldClassName,
          )}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      {(hint || error) && (
        <p className={cn('text-[12px]', error ? 'text-[var(--destructive)]' : 'text-[var(--subtle-foreground)]')}>
          {error || hint}
        </p>
      )}
    </div>
  );
}

type TextareaFieldProps = FieldBaseProps & React.TextareaHTMLAttributes<HTMLTextAreaElement>;

export function TextareaField({
  id,
  label,
  hint,
  error,
  required,
  className,
  fieldClassName,
  ...props
}: TextareaFieldProps) {
  return (
    <div className={cn('space-y-1.5', className)}>
      <label htmlFor={id} className="text-[13px] font-medium text-[var(--muted-foreground)]">
        {label}
      </label>
      <Textarea
        id={id}
        required={required}
        className={cn(
          error ? 'border-[var(--destructive)] focus:border-[var(--destructive)]' : '',
          fieldClassName,
        )}
        {...props}
      />
      {(hint || error) && (
        <p className={cn('text-[12px]', error ? 'text-[var(--destructive)]' : 'text-[var(--subtle-foreground)]')}>
          {error || hint}
        </p>
      )}
    </div>
  );
}
