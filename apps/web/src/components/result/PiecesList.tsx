import type { Piece } from '@ljbu/contracts';
import { useTranslation } from 'react-i18next';
import { StateIcon } from '../StateIcon';
import type { Selection } from './selection';
import styles from './PiecesList.module.css';

interface Props {
  pieces: Piece[];
  selection: Selection;
  onSelect: (selection: Selection) => void;
}

/** Stands in for the 3D model until M5: each piece with its state, selectable. */
export function PiecesList({ pieces, selection, onSelect }: Props) {
  const { t } = useTranslation();
  if (pieces.length === 0) {
    return <p className={styles.empty}>{t('result.piecesEmpty')}</p>;
  }
  return (
    <ul className={styles.list}>
      {pieces.map((piece) => (
        <li key={piece.id}>
          <button
            type="button"
            className={styles.piece}
            data-archetype={piece.archetype}
            data-built={piece.built}
            aria-pressed={selection.pieceId === piece.id}
            onClick={() => {
              onSelect({ pieceId: piece.id, sourceRef: piece.sourceRef });
            }}
          >
            <StateIcon state={piece.state} /> <span className={styles.label}>{piece.label}</span>{' '}
            <span className={styles.meta}>
              {piece.archetype}
              {!piece.built && ` · ${t('result.pieceNotBuilt')}`}
              {piece.target && ` → ${piece.target}`}
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}
